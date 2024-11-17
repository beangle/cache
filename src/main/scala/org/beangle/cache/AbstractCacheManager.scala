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

package org.beangle.cache

import org.beangle.commons.cache.{Cache, CacheManager}

/**
 * @author chaostone
 */
abstract class AbstractCacheManager(val autoCreate: Boolean) extends CacheManager {
  private var registry = Map.empty[String, Cache[_, _]]

  /**
   * Return the cache associated with the given name.
   */
  override final def getCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V] = {
    registry.get(name) match {
      case Some(cache) => cache.asInstanceOf[Cache[K, V]]
      case None =>
        registry.synchronized {
          registry.get(name) match {
            case Some(cache) => cache.asInstanceOf[Cache[K, V]]
            case None =>
              if (autoCreate) {
                val newcache = newCache(name, keyType, valueType)
                registry += (name -> newcache)
                newcache.asInstanceOf[Cache[K, V]]
              } else {
                val existed = findCache(name, keyType, valueType)
                if (null == existed) throw new RuntimeException(s"Cannot find cache $name")
                registry += (name -> existed)
                existed
              }
          }
        }
    }
  }

  protected def register[K, V](name: String, cache: Cache[K, V]): Unit = {
    registry += (name -> cache)
  }

  protected def newCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V]

  protected def findCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V]

}
