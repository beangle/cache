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

import org.beangle.cache.AbstractCacheManager
import org.beangle.commons.cache.Cache
import org.beangle.commons.io.BinarySerializer
import redis.clients.jedis.JedisPool

/**
 * @author chaostone
 */
class RedisCacheManager(pool: JedisPool, serializer: BinarySerializer, autoCreate: Boolean = true)
  extends AbstractCacheManager(autoCreate) {

  var ttl: Int = -1

  protected override def newCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V] = {
    registerClass(keyType, valueType)
    new RedisCache(name, pool, serializer, keyType, valueType, ttl)
  }

  protected override def findCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V] = {
    registerClass(keyType, valueType)
    new RedisCache(name, pool, serializer, keyType, valueType, ttl)
  }

  override def destroy(): Unit = {
    pool.destroy()
  }

  private def registerClass(keyType: Class[_], valueType: Class[_]): Unit = {
    serializer.registerClass(keyType)
    serializer.registerClass(valueType)
  }
}
