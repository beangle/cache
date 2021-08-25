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

package org.beangle.cache.caffeine

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class CaffeineCacheTest extends AnyFunSpec with Matchers {

  describe("CaffeineCache") {
    it("Get or Put") {
      val manager = new CaffeineCacheManager(true)
      val cache = manager.getCache("test", classOf[Long], classOf[String])
      cache.put(1L, "beijing")
      cache.put(2L, "shanghai")
      cache.get(1) should be equals ("beijing")

      assert(cache.get(3) == None)
    }

    it("get cache") {
      val manager = new CaffeineCacheManager(false)
      val cache = manager.getCache("test", classOf[Long], classOf[String])
      assert(null != cache)
      //      val cache2 = manager.getCache("test1", classOf[Long], classOf[String])
      //      assert(null == cache2)
    }
  }
}
