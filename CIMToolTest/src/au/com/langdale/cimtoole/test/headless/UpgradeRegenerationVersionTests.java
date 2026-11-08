package au.com.langdale.cimtoole.test.headless;

import org.osgi.framework.Version;

import au.com.langdale.cimtoole.builder.UpgradeRegeneration;
import junit.framework.TestCase;

/**
 * Tests the version comparison that decides whether a project's artifacts are
 * regenerated after an upgrade (issue #310), including versions written with
 * fewer than three components, such as a release numbered 2.4.
 *
 * <p>
 * These tests need no workspace.
 * </p>
 */
public class UpgradeRegenerationVersionTests extends TestCase {

	public final void testTwoComponentVersionParsesWithZeroMicro() {
		assertEquals("2.4 is 2.4.0", new Version(2, 4, 0), Version.parseVersion("2.4"));
	}

	public final void testOneComponentVersionParsesWithZeros() {
		assertEquals("2 is 2.0.0", new Version(2, 0, 0), Version.parseVersion("2"));
	}

	public final void testMissingStampIsOlder() {
		assertTrue("no stamp", UpgradeRegeneration.isOlder(null, v("2.4")));
	}

	public final void testUnknownRunningVersionNeverTriggers() {
		assertFalse("no running version", UpgradeRegeneration.isOlder(null, null));
		assertFalse("no running version", UpgradeRegeneration.isOlder(v("2.3.0"), null));
	}

	public final void testPatchReleaseToTwoComponentMinorRelease() {
		assertTrue("2.3.0 is older than 2.4", UpgradeRegeneration.isOlder(v("2.3.0"), v("2.4")));
	}

	public final void testTwoComponentStampToPatchRelease() {
		assertTrue("2.4 is older than 2.4.1", UpgradeRegeneration.isOlder(v("2.4"), v("2.4.1")));
	}

	public final void testTwoAndThreeComponentFormsAreEqual() {
		assertFalse("2.4 against 2.4.0", UpgradeRegeneration.isOlder(v("2.4"), v("2.4.0")));
		assertFalse("2.4.0 against 2.4", UpgradeRegeneration.isOlder(v("2.4.0"), v("2.4")));
	}

	public final void testNewerStampDoesNotTrigger() {
		assertFalse("2.4.1 against 2.4", UpgradeRegeneration.isOlder(v("2.4.1"), v("2.4")));
		assertFalse("3 against 2.4", UpgradeRegeneration.isOlder(v("3"), v("2.4")));
	}

	public final void testComponentsCompareNumerically() {
		assertTrue("2.9.3 is older than 2.10", UpgradeRegeneration.isOlder(v("2.9.3"), v("2.10")));
		assertFalse("2.10 is newer than 2.9.3", UpgradeRegeneration.isOlder(v("2.10"), v("2.9.3")));
	}

	public final void testMajorReleaseResetsMinor() {
		assertTrue("2.10.5 is older than 3.0", UpgradeRegeneration.isOlder(v("2.10.5"), v("3.0")));
	}

	public final void testQualifierIsIgnored() {
		assertFalse("same release, different build qualifiers",
				UpgradeRegeneration.isOlder(v("2.4.0.v20261107"), v("2.4.0.v20261201")));
		assertFalse("qualified build of the same release",
				UpgradeRegeneration.isOlder(v("2.4.0"), v("2.4.0.v20261201")));
	}

	private static Version v(String version) {
		return Version.parseVersion(version);
	}
}
