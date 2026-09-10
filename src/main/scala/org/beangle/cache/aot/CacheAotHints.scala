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

package org.beangle.cache.aot

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.jcache.copy.JavaSerializationCopier
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
import org.beangle.cache.AbstractCacheManager
import org.beangle.cache.redis.RedisClientFactory
import org.beangle.commons.aot.{AotHintRegistrar, AotPolicy}

import java.io.File
import java.util.jar.JarFile

/** beangle-cache 的 GraalVM native-image 反射提示。
 *
 * 注册范围：
 *  - jcache/caffeine 二级缓存实现与 jar 全量扫描；
 *  - redis/jedis 连接池与条件绑定 bean；
 *  - 资源项：jedis 版本、caffeine typesafe 配置。
 *
 *  JDK 序列化类型注册已移至 beangle-serializer 模块的 SerializerAotHints；
 *  beangle-jdbc 关键字表由 JdbcAotHints 注册。
 */
class CacheAotHints extends AotHintRegistrar {

  /** Caffeine 策略/节点类运行期经 MethodHandles.findClass 动态解析，
   *  需要声明级构造器、字段与方法。 */
  private val caffeinePolicy = AotPolicy(Set(
    AotPolicy.Category.DeclaredConstructors,
    AotPolicy.Category.DeclaredFields,
    AotPolicy.Category.DeclaredMethods))

  /** Jedis 连接池：commons-pool2 的 BaseGenericObjectPool 按类名 Class.forName
   *  实例化驱逐策略 DefaultEvictionPolicy，GenericObjectPoolMXBean 等经 JMX 反射
   *  查询，ConnectionPool/RedisClient 被 Class.forName 与构造器反射使用。 */
  private val jedisPolicy = AotPolicy(Set(
    AotPolicy.Category.PublicConstructors,
    AotPolicy.Category.PublicMethods))

  override def registering(): Unit = {
    registerCacheImplementations()
    registerJedis()
  }

  /** 注册 jcache/caffeine 二级缓存实现与 caffeine jar 全量扫描。 */
  private def registerCacheImplementations(): Unit = {
    hints.registerType(classOf[AbstractCacheManager], classOf[RedisClientFactory])
    hints.registerType(classOf[JavaSerializationCopier], classOf[CaffeineCachingProvider])
    registerCaffeineStrategies()
  }

  /** 注册 caffeine 包内全部顶层类（策略类 + 节点类）。
   *
   *  LocalCacheFactory.newFactory 在运行期按 builder 配置拼接类名（如 SSMSAW）
   *  再 findClass，组合无法静态推导，因此在构建期枚举 caffeine jar 中的全部
   *  顶层类（排除 $ 嵌套类，嵌套类随外层类可达）。
   */
  private def registerCaffeineStrategies(): Unit = {
    // caffeine jcache 的 TypesafeConfigurator（可达）所需的 typesafe-config 默认配置
    hints.registerPattern("reference.conf")
    hints.registerPattern("application.conf")

    val codeSource = classOf[Caffeine[?, ?]].getProtectionDomain.getCodeSource
    if (codeSource == null) return
    try {
      val jar = new JarFile(new File(codeSource.getLocation.toURI))
      try {
        val entries = jar.entries()
        while entries.hasMoreElements do
          val name = entries.nextElement().getName
          if name.startsWith("com/github/benmanes/caffeine/cache/") && name.endsWith(".class") && !name.contains("$") then
            val className = name.substring(0, name.length - 6).replace('/', '.')
            try {
              val clazz = Class.forName(className, false, classOf[Caffeine[?, ?]].getClassLoader)
              hints.registerType(clazz, caffeinePolicy)
            } catch {
              case _: Throwable => ()
            }
      } finally jar.close()
    } catch {
      case _: Throwable => ()
    }
  }

  /** 注册 redis/jedis：条件绑定 bean 与连接池反射面。 */
  private def registerJedis(): Unit = {
    // RedisClientFactory 是条件绑定 bean：DefaultModule 在构建期 redis 配置为空时
    // bind 宏不执行，需显式注册供 Spring 创建 proxy 的构造器反射
    hints.registerType(classOf[RedisClientFactory])
    // jedis/commons-pool2 为 optional 依赖，按名加载以避免类路径缺失时影响其他注册
    val loader = getClass.getClassLoader
    List(
      "org.apache.commons.pool2.impl.DefaultEvictionPolicy",
      "org.apache.commons.pool2.impl.DefaultPooledObjectInfo",
      "org.apache.commons.pool2.impl.GenericObjectPoolMXBean",
      "redis.clients.jedis.ConnectionPool",
      "redis.clients.jedis.RedisClient"
    ) foreach { n =>
      try hints.registerType(Class.forName(n, false, loader), jedisPolicy)
      catch { case _: Throwable => () }
    }
    // Jedis 启动时经 ClassLoader 读取 jar 内 redis/clients/jedis/pom.properties
    // 打印版本，native 镜像里无该资源（日志报错但不影响功能），显式纳入资源配置
    hints.registerPattern("redis/clients/jedis/pom.properties")
  }
}
