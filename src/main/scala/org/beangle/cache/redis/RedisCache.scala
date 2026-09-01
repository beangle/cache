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

import org.beangle.cache.redis.RedisCache.buildKey
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

  override def get(key: K): Option[V] = {
    val b = client.get(buildKey(name, key).getBytes)
    if (b == null) None else Some(serializer.asObject(vtype, b))
  }

  override def put(key: K, value: V): Unit = {
    setBytes(buildKey(name, key).getBytes, serializer.asBytes(value))
  }

  override def putIfAbsent(key: K, value: V): Boolean = {
    setBytes(buildKey(name, key).getBytes, serializer.asBytes(value), ifAbsent = true)
    false
  }

  override def touch(key: K): Boolean = {
    client.expire(buildKey(name, key).getBytes, ttl) > 0
  }

  def replace(key: K, value: V): Option[V] = {
    val redisKey = buildKey(name, key).getBytes
    val o = client.get(redisKey)
    setBytes(redisKey, serializer.asBytes(value))
    if (o == null) None else Some(serializer.asBytes(o).asInstanceOf[V])
  }

  def replace(key: K, oldvalue: V, newvalue: V): Boolean = {
    val redisKey = buildKey(name, key).getBytes
    val o = client.get(redisKey)
    if (o != null && o == serializer.asBytes(oldvalue)) {
      setBytes(redisKey, serializer.asBytes(newvalue))
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

  /**
   * 向 Redis 写入二进制值。
   *
   * 当 `ifAbsent` 为 true 或实例 `ttl` > 0 时，使用 SET + SetParams 附加选项；
   * 否则执行普通 SET。替代 Jedis 7.x 中已废弃的 setex。
   *
   * @param key Redis 键
   * @param value 要写入的二进制值
   * @param ifAbsent 为 true 时仅当 key 不存在才写入（对应 Redis SET NX）
   */
  private def setBytes(key: Array[Byte], value: Array[Byte], ifAbsent: Boolean = false): Unit = {
    if (ifAbsent || ttl > 0) {
      var params = SetParams.setParams()
      if (ifAbsent) params = params.nx()
      if (ttl > 0) params = params.ex(ttl)
      client.set(key, value, params)
    } else {
      client.set(key, value)
    }
  }
}
