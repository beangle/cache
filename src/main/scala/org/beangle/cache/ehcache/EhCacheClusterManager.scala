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

import org.beangle.cache.{AbstractCacheManager, Broadcaster, BroadcasterBuilder}
import org.beangle.commons.bean.Initializing
import org.beangle.commons.cache.Cache
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder
import org.ehcache.event.EventType

/**
 * @author chaostone
 * Cluster many ehcache using broadcaster
 */
class EhCacheClusterManager(ehManager: EhCacheManager, broadcasterBuilder: BroadcasterBuilder, autoCreate: Boolean)
    extends AbstractCacheManager(autoCreate) with Initializing {

  var propagateExpiration: Boolean = false

  private var broadcaster: Broadcaster = _

  def init(): Unit = {
    broadcaster = broadcasterBuilder.build(ehManager.name, this)
  }

  protected override def newCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V] = {
    val c = ehManager.getEhcache(name, keyType, valueType)
    if (null == c) {
      var builder = ehManager.getConfigBuilder(name + ".Template", keyType, valueType)
      val eventTypes = new java.util.HashSet[EventType]
      eventTypes.addAll(java.util.Arrays.asList(EventType.values: _*))
      if (!propagateExpiration) eventTypes.remove(EventType.EXPIRED)

      val broadcasterConfig = CacheEventListenerConfigurationBuilder
        .newEventListenerConfiguration(classOf[Listener.EvictionBroadcaster], eventTypes)
        .unordered().asynchronous().constructedWith(broadcaster, name)
      builder = builder.withService(broadcasterConfig)
      ehManager.newCache(name, builder.build())
    } else {
      new EhCache(c)
    }
  }

  protected override def findCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V] = {
    ehManager.getCache(name, keyType, valueType)
  }

  override def destroy(): Unit = {
  }
}
