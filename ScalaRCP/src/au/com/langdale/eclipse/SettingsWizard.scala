package au.com.langdale
package eclipse

import scala.actors.Actor._
import actors.Publisher
import db.Oracle.{OracleSpec, OracleTNSSpec, setupTNS}
import db.SettingsAgent._
import db.Spec
import java.sql.SQLException
import eclipse.EventMonitor._

class SettingsWizard(title: String, key: String, val broadcast: Publisher) extends WizardBuilder with Subscriber with UIActor {
  
  setWindowTitle(title)
  var specDirect = OracleSpec( "", "", "", "")
  var specTNS = OracleTNSSpec("", "", "") 
  var useTNS = true
  
  def spec = if(useTNS) specTNS else specDirect
  
  subscribe {
    case s @ Setting(`key`, _) => this ! s
  }
  
  listen {
    case Setting( _, s: OracleSpec) => 
      specDirect = s
      useTNS = false
      doRefresh
      
    case Setting( _,  s: OracleTNSSpec) =>
      specTNS = s
      useTNS = true
      doRefresh
  }
  
  broadcast ! NeedSetting( key )
     
  pages += new DialogBuilder("Database Connection Settings") {

    listen {
      case Update =>
        useTNS = if(useTNS) ! assembly.getButton("direct").getSelection else assembly.getButton("tns").getSelection
        
        if( useTNS ) {
          Log("Updating TNS connection spec")
          import OracleTNSSpec._
          implicit def getUpdateText(prop: Prop[_]) = assembly.getText("tns." + prop.name).getText.trim
          specTNS = OracleTNSSpec( dbName, schema, password) 
        }
        else {
          Log("Updating direct connection spec")
          import OracleSpec._
          implicit def getUpdateText(prop: Prop[_]) = assembly.getText("direct." + prop.name).getText.trim
          specDirect = OracleSpec( host, sid, schema, password) 
        }
        
      case Refresh =>
        assembly.getButton("tns").setSelection(useTNS)
        assembly.getButton("direct").setSelection(! useTNS)
        assembly.showStackLayer((if(useTNS) "tns" else "direct") + ".schema")

        if(useTNS) {
          val mesg = if( setupTNS ) 
            <form/> 
          else 
            <form>
              <p/>
              <p><b>Warning:</b> TNS names service is not available. 
              Check the NVCS_ORACLE_HOME setting.</p>
            </form>
          
          assembly.getMarkup("tns.status").setText(mesg.toString, true, false)
        }
    }

    val template = 
      <grid>
        <group>
          <radiobutton href="tns">Use TNS</radiobutton>
          <radiobutton href="direct">Direct Connection</radiobutton>
        </group>
        <hrule/>
        <stack>
          <grid>
            { detail(OracleTNSSpec, specTNS, "tns.", useTNS) } 
            <display href="tns.status"/>
          </grid>
          <grid>
            { detail(OracleSpec, specDirect, "direct.", ! useTNS) }
          </grid>
        </stack>    
      </grid>
  }
   
  pages += new DialogBuilder("Database Connection Test") with ConnectionTester {

    val template = 
      <grid>
        <displayarea href="mesg" lines="5"/>
        <pushbutton href="start">Test Connection</pushbutton>
      </grid>

    var message = "Click button to test connection."
    var testInProgress = false
    
    listen {

      case Refresh =>
        assembly.setTextValue("mesg", message)
        assembly.getButton("start").setEnabled( ! testInProgress )
        
      case Click("start") =>
        if(! testInProgress ) {
          message = "Connecting to " + spec.uri + " ..."
          testInProgress = true
          assembly.doRefresh
          runTest(spec, localcast)
        }
        
      case TestResult(result) =>
        message += " " + result
        testInProgress = false
        assembly.doRefresh  
    }
  }
  
  def performFinish = {
    broadcast ! Setting( key, spec)
    true
  }
}
