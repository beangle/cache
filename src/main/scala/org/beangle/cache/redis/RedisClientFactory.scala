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
import redis.clients.jedis.{ConnectionPoolConfig, DefaultJedisClientConfig, RedisClient}

object RedisClientFactory {

  def build(props: Map[String, String]): RedisClient = {

    val host = getProperty(props, "host", "127.0.0.1")
    val user = props.getOrElse("user", null)
    val password = props.getOrElse("password", null)
    val port = getProperty(props, "port", 6379)
    val timeout = getProperty(props, "timeout", 2000)
    val database = getProperty(props, "database", 0)

    val clientConfig = DefaultJedisClientConfig.builder().password(password)
      .database(0)
      .timeoutMillis(2000)
      .user(user)
      .password(password)
      .build()

    val poolConfig = new ConnectionPoolConfig
    poolConfig.setMaxIdle(getProperty(props, "minIdle", 2))
    poolConfig.setMinIdle(getProperty(props, "maxIdle", 5))
    poolConfig.setMaxTotal(getProperty(props, "maxTotal", 50))

    val builder = RedisClient.builder()
    builder.hostAndPort(host, port)
      .clientConfig(clientConfig)
      .poolConfig(poolConfig)
      .build()
  }

  private def getProperty(props: Map[String, String], key: String, defaultValue: String): String = {
    props.getOrElse(key, defaultValue).trim()
  }

  private def getProperty(props: Map[String, String], key: String, defaultValue: Int): Int = {
    props.get(key) match {
      case Some(v) => Integer.parseInt(v.trim())
      case None => defaultValue
    }
  }

  private def getProperty(props: Map[String, String], key: String, defaultValue: Boolean): Boolean = {
    props.get(key) match {
      case Some(v) => "true".equalsIgnoreCase(v.trim())
      case None => defaultValue
    }
  }
}

/**
 * @author chaostone
 */
class RedisClientFactory(props: Map[String, String]) extends Factory[RedisClient] {

  val result = RedisClientFactory.build(props)

}
