package au.com.langdale
package db

import scala.actors.Actor._
import scala.actors.Actor
import scala.actors.OutputChannel

import util.Settings
import actors.Publisher.Subscribe

object SettingsAgent {
  case class Setting( key: String, value: Any)
  case class NeedSetting( key: String )
}

import SettingsAgent._

class SettingsAgent( val name: String, notifier: OutputChannel[Any]) extends Actor with Settings {
  
  val description = "Database Connection Settings"
  val dir = System.getProperty("user.home")
  
  def act {
    // Log("SettingsAgent")

    Oracle.setupTNS
  
    loop {
      react {
        case Setting( key, spec: Spec) =>  
          spec.saveTo( key, this)  
          save
        
        case NeedSetting( key ) =>
          Oracle.extractSpec( key, this) match {
            case Some(spec) => notifier ! Setting(key, spec)
            case None =>
          }
          
        case _ =>
      }
    }
  }
}
