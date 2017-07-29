package org.beangle.cache.redis

import org.beangle.commons.io.DefaultBinarySerializer

object RedisCacheTest {
  def main(args: Array[String]): Unit = {
    val pool = JedisPoolFactory.connect(Map.empty)
    val cache = new RedisCache[String, String]("test", pool, DefaultBinarySerializer)
    (0 until 1000) foreach { i =>
      cache.put(i.toString, i.toString)
    }
    println(cache.get("3"))
    println("idle:", pool.getNumIdle)
    println("active:", pool.getNumActive)
    println("await:", pool.getNumWaiters)
  }
}