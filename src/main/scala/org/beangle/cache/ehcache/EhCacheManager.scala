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

import org.beangle.cache.AbstractCacheManager
import org.beangle.commons.bean.Initializing
import org.beangle.commons.cache.Cache
import org.beangle.commons.lang.ClassLoaders
import org.beangle.commons.logging.Logging
import org.ehcache.config.CacheConfiguration
import org.ehcache.config.builders.{CacheConfigurationBuilder, CacheManagerBuilder}
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.spi.service.ServiceCreationConfiguration
import org.ehcache.xml.XmlConfiguration

import java.net.URL

/**
 * @author chaostone
 */
class EhCacheManager(val name: String = "ehcache", autoCreate: Boolean = false) extends AbstractCacheManager(autoCreate)
  with Initializing with Logging {

  var configUrl: URL = _

  private var xmlConfig: XmlConfiguration = _

  private var innerManager: org.ehcache.CacheManager = _

  def init(): Unit = {
    assert(null != name)
    if (null == configUrl) {
      ClassLoaders.getResource(name + ".xml") match {
        case Some(u) => configUrl = u
        case None => logger.warn(s"Cannot find $name.xml in classpath.")
      }
    }
    innerManager =
      if (null != configUrl) {
        xmlConfig = new XmlConfiguration(configUrl)
        CacheManagerBuilder.newCacheManager(xmlConfig)
      } else {
        val config = new DefaultConfiguration(null.asInstanceOf[ClassLoader], Array.empty[ServiceCreationConfiguration[_, _]]: _*)
        CacheManagerBuilder.newCacheManager(config)
      }
    innerManager.init()
  }

  protected override def newCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V] = {
    val c = innerManager.getCache(name, keyType, valueType)
    if (null == c) {
      val builder = getConfigBuilder(name + ".Template", keyType, valueType)
      new EhCache(innerManager.createCache(name, builder.build()))
    } else {
      new EhCache(c)
    }
  }

  protected[ehcache] def newCache[K, V](name: String, config: CacheConfiguration[K, V]): Cache[K, V] = {
    val newer = new EhCache(innerManager.createCache(name, config))
    register(name, newer)
    newer
  }

  protected override def findCache[K, V](name: String, keyType: Class[K], valueType: Class[V]): Cache[K, V] = {
    val ul = innerManager.getCache(name, keyType, valueType)
    if (null == ul) null else new EhCache(ul)
  }

  protected[ehcache] def getEhcache[K, V](name: String, keyType: Class[K], valueType: Class[V]): org.ehcache.Cache[K, V] = {
    innerManager.getCache(name, keyType, valueType)
  }

  protected[ehcache] def getConfigBuilder[K, V](template: String, keyType: Class[K], valueType: Class[V]): CacheConfigurationBuilder[K, V] = {
    var builder: CacheConfigurationBuilder[K, V] = null
    if (null != xmlConfig) builder = xmlConfig.newCacheConfigurationBuilderFromTemplate(template, keyType, valueType)
    //if (null == builder) builder = CacheConfigurationBuilder.newCacheConfigurationBuilder(keyType, valueType, null)
    if (null == builder) {
      throw new RuntimeException("Cannot get a cache config builder for " + template)
    }
    builder

  }

  override def destroy(): Unit = {
    innerManager.close()
  }
}
