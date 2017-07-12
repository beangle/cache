package org.beangle.cache.redis

object RedisCacheTest {
  def main(args: Array[String]): Unit = {
    val pool = JedisPoolFactory.connect(Map.empty)
    val cache = new RedisCache[String, String]("test", pool, new FSTSerializer)
    (0 until 1000) foreach{ i=>
      cache.put(i.toString, i.toString)
    }
    println(cache.get("3"))
    println("idle:",pool.getNumIdle)
    println("active:",pool.getNumActive)
    println("await:",pool.getNumWaiters)
  }
}