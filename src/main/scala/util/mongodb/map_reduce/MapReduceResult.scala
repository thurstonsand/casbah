/**
 * Copyright (c) 2010, Novus Partners, Inc. <http://novus.com>
 *
 * @author Brendan W. McAdams <bmcadams@novus.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTICE: Portions of this work are derived from the Apache License 2.0 "mongo-scala-driver" work
 * by Alexander Azarov <azarov@osinka.ru>, available from http://github.com/alaz/mongo-scala-driver
 */

package com.novus.util.mongodb.map_reduce

import com.mongodb._
import org.scala_tools.javautils.Imports._
import com.novus.util.Logging
import com.novus.util.mongodb._
import Implicits._


class MapReduceResult(resultObj: DBObject)(implicit db: ScalaMongoDB) extends Iterator[DBObject] with Logging {
  log.debug("Map Reduce Result: %s", resultObj)
  // Convert the object to a map to have a quicker, saner shred...
  val result = if (resultObj.containsField("result")) resultObj.get("result") else throw new IllegalArgumentException("Cannot find field 'result' in Map/Reduce Results.")
/*  val result = resultMap.get("result") match {
    case Some(v) => v
    case None => throw new IllegalArgumentException("Cannot find field 'result' in Map/Reduce Results.")
  }*/
  private val resultHandle = db(result.toString).find

  def next(): DBObject = resultHandle.next

  def hasNext: Boolean = resultHandle.hasNext

  def size = resultHandle.count

  private val counts = resultObj.get("counts").asInstanceOf[DBObject]
  // Number of objects scanned
  val input_count: Int = counts.get("input").toString.toInt //, throw new IllegalArgumentException("Cannot find field 'counts.input' in Map/Reduce Results."))
  // Number of times 'emit' was called
  val emit_count: Int = counts.get("emit").toString.toInt//, throw new IllegalArgumentException("Cannot find field 'counts.emit' in Map/Reduce Results."))
  // Number of items in output collection
  val output_count: Int = counts.get("output").toString.toInt//throw new IllegalArgumentException("Cannot find field 'counts.output' in Map/Reduce Results."))

  val timeMillis = resultObj.get("timeMillis").toString.toInt//throw new IllegalArgumentException("Cannot find field 'timeMillis' in Map/Reduce Results."))

  val ok = if (resultObj.get("ok") == 1) true else false

  if (!ok) log.warning("Job result is NOT OK.")


  val err = resultObj.get("err")

  val success = err match {
    case Some(msg) => {
      log.error("Map/Reduce failed: %s", msg)
      false
    }
    case null => {
      log.debug("Map/ Reduce Success.")
      true
    }
  }

  override def toString = {
    if (success) {
      "{MapReduceResult Proxying Result [%s] Handle [%s]}".format(result, resultHandle.toString)
    }
    else {
      "{MapReduceResult - Failure with Error [%s]".format(err.toString)
    }
  }
}