/*
 * Beangle, Agile Development Scaffold and Toolkits.
 *
 * Copyright © 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.cache.redis

import org.beangle.cache.Cache
import org.beangle.commons.io.BinarySerializer
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.SetParams

object RedisCache {

  def buildKey(name: String, key: Any): String = {
    key match {
      case n: Number       => name + ":I:" + n
      case s: CharSequence => name + ":S:" + s
      case o: Any          => name + ":O:" + o
    }
  }
}

/**
 * @author chaostone
 */
class RedisCache[K, V](name: String, pool: JedisPool, serializer: BinarySerializer,
  ktype: Class[K], vtype: Class[V], val ttl: Long = -1)
    extends Cache[K, V] {

  import RedisCache._

  override def get(key: K): Option[V] = {
    val cache = pool.getResource
    try {
      val b = cache.get(buildKey(name, key).getBytes)
      if (b == null) None else Some(serializer.asObject(vtype, b))
    } finally {
      cache.close()
    }
  }

  override def put(key: K, value: V): Unit = {
    val cache = pool.getResource
    try {
      val redisKey = buildKey(name, key).getBytes
      if (ttl > 0) {
        cache.setex(redisKey, ttl, serializer.asBytes(value))
      } else {
        cache.set(redisKey, serializer.asBytes(value))
      }
    } finally {
      cache.close()
    }
  }

  override def putIfAbsent(key: K, value: V): Boolean = {
    val cache = pool.getResource
    try {
      val redisKey = buildKey(name, key).getBytes
      if (ttl > 0) {
        cache.set(redisKey, serializer.asBytes(value), SetParams.setParams().nx().ex(ttl)) == "OK"
      } else {
        cache.set(redisKey, serializer.asBytes(value), SetParams.setParams().nx()) == "OK"
      }
      false
    } finally {
      cache.close()
    }
  }

  override def touch(key: K): Boolean = {
    val cache = pool.getResource
    try {
      cache.expire(buildKey(name, key).getBytes, ttl) > 0
    } finally {
      cache.close()
    }
  }

  def replace(key: K, value: V): Option[V] = {
    val cache = pool.getResource
    try {
      val redisKey = buildKey(name, key).getBytes
      val o = cache.get(redisKey)
      if (ttl > 0) {
        cache.setex(redisKey, ttl, serializer.asBytes(value))
      } else {
        cache.set(redisKey, serializer.asBytes(value))
      }
      if (o == null) None else Some(serializer.asBytes(o).asInstanceOf[V])
    } finally {
      cache.close()
    }
  }

  def replace(key: K, oldvalue: V, newvalue: V): Boolean = {
    val cache = pool.getResource
    try {
      val redisKey = buildKey(name, key).getBytes
      val o = cache.get(redisKey)
      if (o != null && o == serializer.asBytes(oldvalue)) {
        if (ttl > 0) {
          cache.setex(redisKey, ttl, serializer.asBytes(newvalue))
        } else {
          cache.set(redisKey, serializer.asBytes(newvalue))
        }
        true
      } else {
        false
      }
    } finally {
      cache.close()
    }
  }

  override def exists(key: K): Boolean = {
    val cache = pool.getResource
    try {
      cache.exists(buildKey(name, key).getBytes)
    } finally {
      cache.close()
    }
  }

  override def evict(key: K): Boolean = {
    val cache = pool.getResource
    try {
      cache.del(buildKey(name, key)) > 0
    } finally {
      cache.close()
    }
  }

  override def clear(): Unit = {
    val cache = pool.getResource
    try {
      val keys = cache.keys(name + ":*").asInstanceOf[java.util.List[_]]
      cache.del(keys.toArray.asInstanceOf[Array[String]]: _*)
    } finally {
      cache.close()
    }
  }

  override def tti: Long = {
    ttl
  }
}
