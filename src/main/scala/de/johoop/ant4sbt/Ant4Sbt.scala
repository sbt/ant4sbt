/*
 * This file is part of ant4sbt.
 *
 * Copyright (c) 2012 Joachim Hofer
 * All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.johoop.ant4sbt

import sbt._
import sbt.Keys._
import de.johoop.ant4sbt.ant.AntServer
import de.johoop.ant4sbt.ant.AntClient

object Ant4Sbt extends Plugin with Settings {

  override def restartAnt(buildFile: File, baseDir: File, port: Int) = {
    stopAnt(port)
    startAnt(buildFile, baseDir, port)
  }

  override def startAnt(buildFile: File, baseDir: File, port: Int) = {
    // FIXME fork this as a process
    new Thread(new Runnable {
      override def run = {
        AntServer.main(Array(buildFile.getAbsolutePath, baseDir.getAbsolutePath, port.toString))
      }
    }).start
  }

  override def stopAnt(port: Int) = new AntClient(port).stopServer



  val defaultAntHome =
    Option(System getProperty "ant.home") getOrElse (Option(System getenv "ANT_HOME") getOrElse "<invalid ant.home>")

  def antSettings(buildFile: File, baseDir: File = file("."), port: Int = 21345, antHome: String = defaultAntHome) : Seq[Setting[_]]= {

    println("java -cp /work/misc/ant-plugin/target/scala-2.9.1/sbt-0.11.2/ant4sbt-1.0.0-SNAPSHOT.jar:%s/lib/ant.jar:%1$s/lib/ant-launcher.jar:/home/joachim/.ivy2/cache/org.scala-lang/scala-library/jars/scala-library-2.9.1.jar de.johoop.ant4sbt.ant.AntServer %s %s %d".format(
        antHome, buildFile.absolutePath, baseDir.absolutePath, port))

    lazy val antClient = new AntClient(port)

    antClient.targets map { antTarget =>
      TaskKey[Unit]("ant-" + antTarget) <<= streams map { streams =>
        antClient runTarget (antTarget, streams.log)
      }
    } toSeq
  }

  def ant(targetName: String) = TaskKey[Unit]("ant-" + targetName)
}
