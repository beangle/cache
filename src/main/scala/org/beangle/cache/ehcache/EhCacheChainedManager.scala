/*
 * Copyright (C) 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.beangle.cache.ehcache

import org.beangle.cache.chain.{ChainedCache, ChainedManager}
import org.beangle.cache.{AbstractCacheManager, Broadcaster, BroadcasterBuilder}
import org.beangle.commons.bean.Initializing
import org.beangle.commons.cache.*
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder
import org.ehcache.event.EventType

import java.util as ju

/**
 * @author chaostone
 */
class EhCacheChainedManager(ehManager: EhCacheManager, targetManager: CacheManager, autoCreate: Boolean)
  extends AbstractCacheManager(autoCreate) with Initializing with ChainedManager {

  var propagateExpiration: Boolean = false
  var broadcasterBuilder: BroadcasterBuilder = _
  private var broadcaster: Broadcaster = _

  def init(): Unit = {
    broadcaster = broadcasterBuilder.build(ehManager.name, ehManager)
  }

  protected override def findCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V] = {
    val c = ehManager.getEhcache(name, keyType, valueType)
    if (null != c) {
      val targetCache = targetManager.getCache(name, keyType, valueType)
      new ChainedCache(new EhCache(c), targetCache)
    } else {
      null
    }
  }

  protected override def newCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V] = {
    val targetCache = targetManager.getCache(name, keyType, valueType)
    val c = ehManager.getEhcache(name, keyType, valueType)
    if (null != c) {
      new ChainedCache(new EhCache(c), targetCache)
    } else {
      var builder = ehManager.getConfigBuilder(name + ".Template", keyType, valueType)

      if (null != broadcaster) {
        val eventTypes = new java.util.HashSet[EventType]
        eventTypes.addAll(ju.Arrays.asList(EventType.values: _*))
        if (!propagateExpiration) eventTypes.remove(EventType.EXPIRED)

        val broadcasterConfig = CacheEventListenerConfigurationBuilder
          .newEventListenerConfiguration(classOf[Listener.EvictionBroadcaster], eventTypes)
          .unordered().asynchronous().constructedWith(broadcaster, name)
        builder = builder.withService(broadcasterConfig)
      }
      //如果需要传播过期事件，需要再次构造builder
      if (propagateExpiration) {
        val expireListenerConfig = CacheEventListenerConfigurationBuilder
          .newEventListenerConfiguration(classOf[Listener.ChainExpiry], EventType.EXPIRED)
          .unordered().asynchronous().constructedWith(targetCache)
        builder = builder.withService(expireListenerConfig)
      }
      val newer = ehManager.newCache(name, builder.build())
      new ChainedCache(newer, targetCache)
    }
  }

  override def destroy(): Unit = {
  }
}
