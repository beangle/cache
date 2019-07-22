/*
 * Beangle, Agile Development Scaffold and Toolkits.
 *
 * Copyright © 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.cache.jgroups

import java.net.URL

import org.beangle.cache.{Broadcaster, BroadcasterBuilder, CacheManager, EvictMessage}
import org.beangle.commons.bean.Initializing
import org.beangle.commons.io.BinarySerializer
import org.beangle.commons.logging.Logging
import org.jgroups.{JChannel, Message, ReceiverAdapter}

class JGroupsBroadcasterBuilder(networkConfigUrl: URL, serializer: BinarySerializer) extends BroadcasterBuilder {
  def build(channel: String, localManager: CacheManager): Broadcaster = {
    val broadcaster = new JGroupsBroadcaster(channel, new JChannel(networkConfigUrl), serializer, localManager)
    broadcaster.init()
    broadcaster
  }
}

/**
  * @author chaostone
  */
class JGroupsBroadcaster(channelName: String, channel: JChannel, serializer: BinarySerializer, localManager: CacheManager)
  extends ReceiverAdapter with Broadcaster with Initializing with Logging {

  def init(): Unit = {
    channel.setReceiver(this)
    channel.connect(this.channelName)
  }

  override def receive(msg: Message): Unit = {
    if (msg.getSrc.equals(channel.getAddress)) return
    val buffer = msg.getBuffer
    if (buffer.nonEmpty) {
      val msg = serializer.asObject(classOf[EvictMessage], buffer)
      if (!msg.isIssueByLocal) {
        if (null == msg.key) {
          localManager.getCache(msg.cache, classOf[Any], classOf[Any]).clear()
        } else {
          localManager.getCache(msg.cache, classOf[Any], classOf[Any]).evict(msg.key)
        }
      }
    }
  }

  override def publishEviction(cache: String, key: Any): Unit = {
    try {
      channel.send(new Message(null, serializer.asBytes(new EvictMessage(cache, key))))
    } catch {
      case e: Throwable =>
        logger.error("Unable to evict,cache=" + cache + " key=" + key, e);
    }
  }

  override def publishClear(cache: String): Unit = {
    try {
      channel.send(new Message(null, serializer.asBytes(new EvictMessage(cache, null))))
    } catch {
      case e: Throwable =>
        logger.error("Unable to clear cache :" + cache, e);
    }
  }

}
