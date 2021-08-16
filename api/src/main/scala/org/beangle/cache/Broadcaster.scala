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

/**
  * @author chaostone
  */
trait Broadcaster {
  def publishEviction(cache: String, key: Any): Unit

  def publishClear(cache: String): Unit
}

trait BroadcasterBuilder {
  def build(channel: String, localManager: CacheManager): Broadcaster
}

object EvictMessage {
  val Eviction: Byte = 0.asInstanceOf[Byte]
  val Clear: Byte = 1.asInstanceOf[Byte]
  val LocalIssuer: Int = new scala.util.Random(System.currentTimeMillis).nextInt(1000000)
}

class EvictMessage(val cache: String, val key: Any) extends Serializable {

  import EvictMessage._

  var operation: Byte = Eviction
  var issuer: Int = LocalIssuer

  def isIssueByLocal: Boolean = {
    issuer == LocalIssuer
  }

  override def toString: String = {
    if (operation == Eviction) "clear" + cache else s"evict $key  in $cache"
  }
}
