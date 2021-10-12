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

import org.beangle.commons.bean.Factory
import redis.clients.jedis.{JedisPool, JedisPoolConfig}

import java.time.Duration

object JedisPoolFactory {

  def connect(props: Map[String, String]): JedisPool = {
    val config = new JedisPoolConfig()
    val host = getProperty(props, "host", "127.0.0.1")
    val password = props.getOrElse("password", null)
    val port = getProperty(props, "port", 6379)
    val timeout = getProperty(props, "timeout", 2000)
    val database = getProperty(props, "database", 0)

    config.setBlockWhenExhausted(getProperty(props, "blockWhenExhausted", true))
    config.setMaxIdle(getProperty(props, "maxIdle", 10))
    config.setMinIdle(getProperty(props, "minIdle", 5))
    config.setMaxTotal(getProperty(props, "maxTotal", 100))
    config.setMaxWaitMillis(getProperty(props, "maxWait", 50))
    config.setTestWhileIdle(getProperty(props, "testWhileIdle", false))
    config.setTestOnBorrow(getProperty(props, "testOnBorrow", true))
    config.setTestOnReturn(getProperty(props, "testOnReturn", false))
    config.setNumTestsPerEvictionRun(getProperty(props, "numTestsPerEvictionRun", 10))
    config.setSoftMinEvictableIdleTime(Duration.ofMillis(getProperty(props, "softMinEvictableIdleTimeMillis", 10)))
    config.setTimeBetweenEvictionRuns(Duration.ofMillis(getProperty(props, "timeBetweenEvictionRunsMillis", 10)))
    config.setLifo(getProperty(props, "lifo", false))

    new JedisPool(config, host, port, timeout, password, database)
  }
  private def getProperty(props: Map[String, String], key: String, defaultValue: String): String = {
    props.getOrElse(key, defaultValue).trim()
  }

  private def getProperty(props: Map[String, String], key: String, defaultValue: Int): Int = {
    props.get(key) match {
      case Some(v) => Integer.parseInt(v.trim())
      case None    => defaultValue
    }
  }

  private def getProperty(props: Map[String, String], key: String, defaultValue: Boolean): Boolean = {
    props.get(key) match {
      case Some(v) => "true".equalsIgnoreCase(v.trim())
      case None    => defaultValue
    }
  }
}
/**
 * @author chaostone
 */
class JedisPoolFactory(props: Map[String, String]) extends Factory[JedisPool] {

  private val pool = JedisPoolFactory.connect(props)

  def result: JedisPool = {
    pool
  }

}
