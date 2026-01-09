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

import org.beangle.commons.cache.Cache
import org.beangle.commons.io.BinarySerializer
import redis.clients.jedis.RedisClient
import redis.clients.jedis.params.SetParams

object RedisCache {

  def buildKey(name: String, key: Any): String = {
    key match {
      case n: Number => name + ":I:" + n
      case s: CharSequence => name + ":S:" + s
      case o: Any => name + ":O:" + o
    }
  }
}

/**
 * @author chaostone
 */
class RedisCache[K, V](name: String, client: RedisClient, serializer: BinarySerializer,
                       ktype: Class[K], vtype: Class[V], val ttl: Long = -1)
  extends Cache[K, V] {

  import RedisCache.*

  override def get(key: K): Option[V] = {
    val b = client.get(buildKey(name, key).getBytes)
    if (b == null) None else Some(serializer.asObject(vtype, b))
  }

  override def put(key: K, value: V): Unit = {
    val redisKey = buildKey(name, key).getBytes
    if (ttl > 0) {
      client.setex(redisKey, ttl, serializer.asBytes(value))
    } else {
      client.set(redisKey, serializer.asBytes(value))
    }
  }

  override def putIfAbsent(key: K, value: V): Boolean = {
    val redisKey = buildKey(name, key).getBytes
    if (ttl > 0) {
      client.set(redisKey, serializer.asBytes(value), SetParams.setParams().nx().ex(ttl)) == "OK"
    } else {
      client.set(redisKey, serializer.asBytes(value), SetParams.setParams().nx()) == "OK"
    }
    false
  }

  override def touch(key: K): Boolean = {
    client.expire(buildKey(name, key).getBytes, ttl) > 0
  }

  def replace(key: K, value: V): Option[V] = {
    val redisKey = buildKey(name, key).getBytes
    val o = client.get(redisKey)
    if (ttl > 0) {
      client.setex(redisKey, ttl, serializer.asBytes(value))
    } else {
      client.set(redisKey, serializer.asBytes(value))
    }
    if (o == null) None else Some(serializer.asBytes(o).asInstanceOf[V])
  }

  def replace(key: K, oldvalue: V, newvalue: V): Boolean = {
    val redisKey = buildKey(name, key).getBytes
    val o = client.get(redisKey)
    if (o != null && o == serializer.asBytes(oldvalue)) {
      if (ttl > 0) {
        client.setex(redisKey, ttl, serializer.asBytes(newvalue))
      } else {
        client.set(redisKey, serializer.asBytes(newvalue))
      }
      true
    } else {
      false
    }
  }

  override def exists(key: K): Boolean = {
    client.exists(buildKey(name, key).getBytes)
  }

  override def evict(key: K): Boolean = {
    client.del(buildKey(name, key)) > 0
  }

  override def clear(): Unit = {
    val keys = client.keys(name + ":*").asInstanceOf[java.util.List[_]]
    client.del(keys.toArray.asInstanceOf[Array[String]]: _*)
  }

  override def tti: Long = {
    ttl
  }
}
