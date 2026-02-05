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

package org.beangle.cache.redis

import org.beangle.cache.chain.ChainedManager
import org.beangle.cache.redis.SubscriberDaemon.*
import org.beangle.cache.{Broadcaster, BroadcasterBuilder, CacheLogger, EvictMessage}
import org.beangle.commons.bean.Initializing
import org.beangle.commons.cache.CacheManager
import org.beangle.commons.concurrent.Locks
import org.beangle.commons.io.BinarySerializer
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.util.SafeEncoder
import redis.clients.jedis.{BinaryJedisPubSub, RedisClient}

import java.util.concurrent.locks.ReentrantLock

class RedisBroadcasterBuilder(client: RedisClient, serializer: BinarySerializer) extends BroadcasterBuilder {
  def build(channel: String, localManager: CacheManager): Broadcaster = {
    if (localManager.isInstanceOf[ChainedManager]) {
      throw new RuntimeException("Local cache manager couldn't be chained.")
    }
    val broadcaster = new RedisBroadcaster(SafeEncoder.encode(channel), client, serializer, localManager)
    broadcaster.init()
    broadcaster
  }
}

object SubscriberDaemon {
  private var running = false
  private val runningLock = new ReentrantLock()
}

/**
 * Subscribe and on receive message thread
 */
class SubscriberDaemon(client: RedisClient, broadcaster: RedisBroadcaster, channel: Array[Byte]) extends Runnable {
  override def run(): Unit = {
    Locks.withLock(runningLock) {
      if (running) {
        CacheLogger.warn("SubscriberDaemon is running,opereration aborted.")
      } else {
        running = true
        var i = 0
        while (true) {
          try {
            CacheLogger.info("Subscribing redis on channel:" + SafeEncoder.encode(channel))
            client.subscribe(broadcaster, channel)
          } catch {
            case e: JedisConnectionException =>
              e.printStackTrace()
              i += 1
              if (i % 5 == 0) {
                CacheLogger.error("Connect redis failed after 5 tries.")
                Thread.sleep(100000)
              }
          }
        }
      }
    }
  }
}

/**
 * @author chaostone
 */
class RedisBroadcaster(channel: Array[Byte], client: RedisClient, serializer: BinarySerializer, localManager: CacheManager)
  extends BinaryJedisPubSub with Broadcaster with Initializing {

  var subscriber: Thread = _

  def init(): Unit = {
    //the subscription will block current thread,so we start a new one.
    subscriber = new Thread(new SubscriberDaemon(client, this, channel))
    subscriber.setName("RedisSubscriberDaemon")
    subscriber.setDaemon(true)
    subscriber.start()
  }

  override def onMessage(channel: Array[Byte], message: Array[Byte]): Unit = {
    val msg = serializer.asObject(classOf[EvictMessage], message)
    if (!msg.isIssueByLocal) {
      val cache = localManager.getCache(msg.cache, classOf[Any], classOf[Any])
      if (null != cache) {
        if (msg.operation == EvictMessage.Clear) {
          cache.clear()
        } else {
          cache.evict(msg.key)
        }
      }
    }
  }

  override def publishEviction(cache: String, key: Any): Unit = {
    client.publish(channel, serializer.asBytes(new EvictMessage(cache, key)))
  }

  override def publishClear(cache: String): Unit = {
    client.publish(channel, serializer.asBytes(new EvictMessage(cache, null)))
  }

}
