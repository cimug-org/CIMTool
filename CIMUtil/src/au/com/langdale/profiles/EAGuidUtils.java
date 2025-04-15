/**
 * 
 */
package au.com.langdale.profiles;

/**
 * 
 */
public final class EAGuidUtils {

	private EAGuidUtils() {
	}
	
	/** 
	 * It was discovered that CIMTool has an issue in the specific parser designed to process 
	 * .eap schema files. The issue does not exist in the new implementation that parses the 
	 * new 64-bit .qea schema files nor in the parser that processes .XMI schema files for a 
	 * project. The issue identified is related to the UUIDs that are used to uniquely identify 
	 * classes, enumerations, attributes, associations,etc. The format for the identifiers is  
	 * different than the typical format for a UUID and instead in EA appears for example as
	 * shown next whereby a UUID e.g. {9F8964F1-6C32-465b-A83D-F5A201A291C3} is pre-pended with 
	 * a leading 'EAID_' prefix and the dashes in the UUID replaced with underscores: 
	 * 
	 * 	{9F8964F1-6C32-465b-A83D-F5A201A291C3} --> EAID_9F8964F1_6C32_465b_A83D_F5A201A291C3
	 * 
	 * Within , the XUID format was used when it was originally implemented (e.g. 9F8964F1-6C32-465b-A83D-F5A201A291C3 with dashes and no EAID_ 
	 * prefix). ID is utilized when generating the .OWL profile so can not be corrected without 
	 * breaking backwards compatibility. Therefore the following method is utilized to "fix" 
	 * "ea_guid"s.
	*/
	public static String fixEAGuid(String eaGUID) {
		if (eaGUID != null && !eaGUID.startsWith("EAID")) {
			/**
			 *  This initial check for "-A" or "-B" is specifically related to association 
			 *  role ends which are purely a convention within CIMTool and nothing more than
			 *  a simple appending of such a suffix to EA's EAID of the association. This is 
			 *  because role ends are not differentiated in EA so CIMTool simply needs a means 
			 *  to distinguish between the role ends.
			 */
			if (eaGUID.endsWith("-A") || eaGUID.endsWith("-B")) {
				return 	"EAID" + eaGUID.substring(0, eaGUID.length() - 2).replace("-", "_") + eaGUID.substring(eaGUID.length() - 2);
			} else {
				return "EAID" + eaGUID.replace("-", "_");
			}
		}
		return eaGUID;
	}
	
	public static String fixPackageEAGuid(String eaGUID) {
		if (eaGUID != null && !eaGUID.startsWith("EAPK")) {
			return "EAPK" + eaGUID.replace("-", "_");
		}
		return eaGUID;
	}
	
}
