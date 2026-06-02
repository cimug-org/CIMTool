using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.IO;
using System.Reflection;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata;

var projectDirectory = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", ".."));
var profilesDirectory = Path.GetFullPath(Path.Combine(projectDirectory, "..", "CSharpEFTestProject", "Profiles"));
var GeneratedCSharpPath = Path.Combine(profilesDirectory, "SampleProfile.csharp-ef-rdfs.cs");
var SqlPath = Path.Combine(profilesDirectory, "SampleProfile.rdfs-ansi92.sql");

var completedSections = new List<string>();
var diagnostics = new List<string>();

RunSection("Reflection Contract", VerifyReflectionContract);
RunSection("EF Metadata Contract", VerifyEfMetadataContract);
RunSection("SQL Schema Parity", VerifySqlSchemaParity);
RunSection("Generated CSharp Text Integrity", VerifyGeneratedCSharpTextIntegrity);
RunSection("Lookup Equality Semantics", VerifyLookupEqualitySemantics);
RunSection("Name Association Behavior", VerifyNameAssociationBehavior);
RunSection("Parent Organisation Delete Guard", VerifyParentOrganisationDeleteGuard);
RunSection("Generated Compound Replacement Cleanup", VerifyGeneratedCompoundReplacementCleanup);
RunSection("Generated Null Detach Cleanup", VerifyGeneratedNullDetachCleanup);
RunSection("Generated Compound Async Cleanup", () => VerifyGeneratedCompoundAsyncCleanup().GetAwaiter().GetResult());
RunSection("Generated Compoundless Entity Cleanup", VerifyCompoundlessEntityCleanup);
RunSection("Generated Mapping Baseline", VerifyGeneratedMappingBaseline);
RunSection("Inheritance Storage", VerifyInheritanceStorage);
RunSection("Inheritance Delete Cleanup", VerifyInheritanceDeleteCleanup);

Console.WriteLine($"Comprehensive EF Core regression test passed ({completedSections.Count} sections).");
foreach (var section in completedSections)
{
    Console.WriteLine($"- {section}");
}

if (diagnostics.Count > 0)
{
    Console.WriteLine("Diagnostics:");
    foreach (var diagnostic in diagnostics)
    {
        Console.WriteLine($"- {diagnostic}");
    }
}

void RunSection(string name, Action action)
{
    action();
    completedSections.Add(name);
}

void RunSectionAsync(string name, Func<Task> action)
{
    action().GetAwaiter().GetResult();
    completedSections.Add(name);
}

void VerifyReflectionContract()
{
    AssertPropertyKey(typeof(SampleProfile.IdentifiedObject), nameof(SampleProfile.IdentifiedObject.MRId), "mRID", 100);
    AssertPropertyKey(typeof(SampleProfile.Name), nameof(SampleProfile.Name.Id), "id", 100);
    AssertPropertyKey(typeof(SampleProfile.ElectronicAddress), nameof(SampleProfile.ElectronicAddress.Id), "id", 100);
    AssertPropertyKey(typeof(SampleProfile.TelephoneNumber), nameof(SampleProfile.TelephoneNumber.Id), "id", 100);
    AssertPropertyKey(typeof(SampleProfile.StreetAddress), nameof(SampleProfile.StreetAddress.Id), "id", 100);
    AssertPropertyKey(typeof(SampleProfile.Status), nameof(SampleProfile.Status.Id), "id", 100);

    AssertNaturalKey(typeof(SampleProfile.CrewStatusKind), nameof(SampleProfile.CrewStatusKind.Name), "name", 100);
    AssertNaturalKey(typeof(SampleProfile.PhaseCode), nameof(SampleProfile.PhaseCode.Name), "name", 100);
    AssertNaturalKey(typeof(SampleProfile.WireInsulationKind), nameof(SampleProfile.WireInsulationKind.Name), "name", 100);
    AssertNaturalKey(typeof(SampleProfile.WireMaterialKind), nameof(SampleProfile.WireMaterialKind.Name), "name", 100);

    AssertNoDeclaredKeyProperties(typeof(SampleProfile.AssetInfo));
    AssertNoDeclaredKeyProperties(typeof(SampleProfile.Organisation));
    AssertNoDeclaredKeyProperties(typeof(SampleProfile.ParentOrganization));
    AssertNoDeclaredKeyProperties(typeof(SampleProfile.WireInfo));
    AssertNoDeclaredKeyProperties(typeof(SampleProfile.OverheadWireInfo));

    var organisationIndexes = typeof(SampleProfile.Organisation).GetCustomAttributes<IndexAttribute>().ToList();
    AssertCondition(organisationIndexes.Count == 5, "Expected five unique Organisation compound indexes.");
    AssertCondition(HasSinglePropertyUniqueIndex(organisationIndexes, nameof(SampleProfile.Organisation.ElectronicAddressId)),
        "Expected Organisation.ElectronicAddressId to carry a unique index.");
    AssertCondition(HasSinglePropertyUniqueIndex(organisationIndexes, nameof(SampleProfile.Organisation.Phone1Id)),
        "Expected Organisation.Phone1Id to carry a unique index.");
    AssertCondition(HasSinglePropertyUniqueIndex(organisationIndexes, nameof(SampleProfile.Organisation.Phone2Id)),
        "Expected Organisation.Phone2Id to carry a unique index.");
    AssertCondition(HasSinglePropertyUniqueIndex(organisationIndexes, nameof(SampleProfile.Organisation.PostalAddressId)),
        "Expected Organisation.PostalAddressId to carry a unique index.");
    AssertCondition(HasSinglePropertyUniqueIndex(organisationIndexes, nameof(SampleProfile.Organisation.StreetAddressId)),
        "Expected Organisation.StreetAddressId to carry a unique index.");

    var dbSetNames = typeof(SampleProfile.DbContextBase)
        .GetProperties(BindingFlags.Instance | BindingFlags.Public)
        .Where(p => p.PropertyType.IsGenericType && p.PropertyType.GetGenericTypeDefinition() == typeof(DbSet<>))
        .Select(p => p.Name)
        .ToHashSet(StringComparer.Ordinal);

    foreach (var expected in new[]
    {
        "IdentifiedObjects",
        "Names",
        "AssetInfos",
        "Organisations",
        "ParentOrganizations",
        "WireInfos",
        "WireSpacingInfos",
        "OverheadWireInfos"
    })
    {
        AssertCondition(dbSetNames.Contains(expected), $"Expected DbContextBase to expose DbSet '{expected}'.");
    }

    foreach (var unexpected in new[] { "ElectronicAddresses", "TelephoneNumbers", "StreetAddresses", "Statuses" })
    {
        AssertCondition(!dbSetNames.Contains(unexpected),
            $"Did not expect compound DbSet '{unexpected}' to be publicly exposed from DbContextBase.");
    }

    var optionsCtorExists = typeof(SampleProfile.DbContextBase)
        .GetConstructors(BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic)
        .Any(ctor =>
        {
            var parameters = ctor.GetParameters();
            return parameters.Length == 1 && typeof(DbContextOptions).IsAssignableFrom(parameters[0].ParameterType);
        });

    AssertCondition(optionsCtorExists,
        "Expected DbContextBase to expose a protected DbContextOptions constructor for dependency injection registration.");
}

void VerifyEfMetadataContract()
{
    WithFreshDatabase(connection =>
    {
        using var context = new SampleProfileDbContext(connection);

        AssertPrimaryKey(context, typeof(SampleProfile.IdentifiedObject), nameof(SampleProfile.IdentifiedObject.MRId));
        AssertPrimaryKey(context, typeof(SampleProfile.Name), nameof(SampleProfile.Name.Id));
        AssertPrimaryKey(context, typeof(SampleProfile.ElectronicAddress), nameof(SampleProfile.ElectronicAddress.Id));
        AssertPrimaryKey(context, typeof(SampleProfile.StreetAddress), nameof(SampleProfile.StreetAddress.Id));
        AssertPrimaryKey(context, typeof(SampleProfile.CrewStatusKind), nameof(SampleProfile.CrewStatusKind.Name));
        AssertPrimaryKey(context, typeof(SampleProfile.PhaseCode), nameof(SampleProfile.PhaseCode.Name));
        AssertPrimaryKey(context, typeof(SampleProfile.WireInsulationKind), nameof(SampleProfile.WireInsulationKind.Name));
        AssertPrimaryKey(context, typeof(SampleProfile.WireMaterialKind), nameof(SampleProfile.WireMaterialKind.Name));

        AssertTableName(context, typeof(SampleProfile.IdentifiedObject), "IdentifiedObject");
        AssertTableName(context, typeof(SampleProfile.Name), "Name");
        AssertTableName(context, typeof(SampleProfile.Organisation), "Organisation");
        AssertTableName(context, typeof(SampleProfile.ParentOrganization), "ParentOrganization");
        AssertTableName(context, typeof(SampleProfile.OverheadWireInfo), "OverheadWireInfo");

        AssertColumnName(context, typeof(SampleProfile.Name), nameof(SampleProfile.Name.NameValue), "name");
        AssertColumnName(context, typeof(SampleProfile.Name), nameof(SampleProfile.Name.IdentifiedObjectId), "IdentifiedObject");
        AssertColumnName(context, typeof(SampleProfile.IdentifiedObject), nameof(SampleProfile.IdentifiedObject.NameValue), "name");

        AssertDeleteBehavior(context, typeof(SampleProfile.Name), nameof(SampleProfile.Name.IdentifiedObjectId), DeleteBehavior.ClientNoAction);
        AssertDeleteBehavior(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.ParentOrganisationId), DeleteBehavior.ClientNoAction);
        AssertDeleteBehavior(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.ElectronicAddressId), DeleteBehavior.Restrict);
        AssertDeleteBehavior(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.Phone1Id), DeleteBehavior.Restrict);
        AssertDeleteBehavior(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.Phone2Id), DeleteBehavior.Restrict);
        AssertDeleteBehavior(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.PostalAddressId), DeleteBehavior.Restrict);
        AssertDeleteBehavior(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.StreetAddressId), DeleteBehavior.Restrict);
        AssertDeleteBehavior(context, typeof(SampleProfile.StreetAddress), nameof(SampleProfile.StreetAddress.StatusId), DeleteBehavior.Restrict);
        AssertDeleteBehavior(context, typeof(SampleProfile.StreetAddress), nameof(SampleProfile.StreetAddress.StreetDetailId), DeleteBehavior.Restrict);
        AssertDeleteBehavior(context, typeof(SampleProfile.StreetAddress), nameof(SampleProfile.StreetAddress.TownDetailId), DeleteBehavior.Restrict);

        AssertUniqueIndex(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.ElectronicAddressId));
        AssertUniqueIndex(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.Phone1Id));
        AssertUniqueIndex(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.Phone2Id));
        AssertUniqueIndex(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.PostalAddressId));
        AssertUniqueIndex(context, typeof(SampleProfile.Organisation), nameof(SampleProfile.Organisation.StreetAddressId));

        AssertForeignKeyTarget(context, typeof(SampleProfile.Name), nameof(SampleProfile.Name.IdentifiedObjectId),
            typeof(SampleProfile.IdentifiedObject), nameof(SampleProfile.IdentifiedObject.MRId));
    });
}

void VerifySqlSchemaParity()
{
    var sql = File.ReadAllText(SqlPath);

    AssertCondition(sql.Contains("CREATE TABLE \"Name\""), "Expected SQL to declare the Name table.");
    AssertCondition(sql.Contains("\"IdentifiedObject\" VARCHAR(100)"), "Expected SQL to store the Name.IdentifiedObject foreign key.");
    AssertCondition(sql.Contains("ALTER TABLE \"Name\" ADD FOREIGN KEY ( \"IdentifiedObject\" ) REFERENCES \"IdentifiedObject\" ( \"mRID\" );"),
        "Expected SQL to point Name.IdentifiedObject to IdentifiedObject.mRID.");
    AssertCondition(sql.Contains("CREATE TABLE \"Organisation\""), "Expected SQL to declare the Organisation table.");
    AssertCondition(sql.Contains("\"electronicAddress\" VARCHAR(100) UNIQUE"), "Expected Organisation.electronicAddress UNIQUE in SQL.");
    AssertCondition(sql.Contains("\"phone1\" VARCHAR(100) UNIQUE"), "Expected Organisation.phone1 UNIQUE in SQL.");
    AssertCondition(sql.Contains("ALTER TABLE \"TelephoneNumber\" ADD CONSTRAINT fk_TelephoneNumber_Organisation_phone1 FOREIGN KEY ( \"id\" ) REFERENCES \"Organisation\" ( \"phone1\" ) ON DELETE CASCADE;"),
        "Expected SQL reverse cascade for Organisation.phone1 compound cleanup.");
    AssertCondition(sql.Contains("ALTER TABLE \"Status\" ADD CONSTRAINT fk_Status_StreetAddress_status FOREIGN KEY ( \"id\" ) REFERENCES \"StreetAddress\" ( \"status\" ) ON DELETE CASCADE;"),
        "Expected SQL reverse cascade for StreetAddress.status compound cleanup.");
    AssertCondition(sql.Contains("CREATE INDEX ix_Name_IdentifiedObject ON \"Name\" ( \"IdentifiedObject\" );"),
        "Expected SQL index on Name.IdentifiedObject.");

    diagnostics.Add("PARITY GAP OBSERVED: SQL models compound ownership with reverse ON DELETE CASCADE constraints, while generated EF Core uses DeleteBehavior.Restrict plus SaveChanges cleanup.");
}

void VerifyGeneratedCSharpTextIntegrity()
{
    var generated = File.ReadAllText(GeneratedCSharpPath);

    AssertCondition(generated.StartsWith("// ============================================================"),
        "Expected generated C# to start with the banner comment, not XML.");
    AssertCondition(generated.Contains("public class Name"), "Expected generated C# to include the Name entity.");
    AssertCondition(generated.Contains("public abstract class DbContextBase : DbContext"),
        "Expected generated C# to include DbContextBase.");
    AssertCondition(generated.Contains("public DbSet<SampleProfile.Name> Names => Set<SampleProfile.Name>();"),
        "Expected generated C# to expose Names from DbContextBase.");
    AssertCondition(generated.Contains("DeleteOrphanedCompounds"),
        "Expected generated C# to include compound cleanup helpers.");
    AssertCondition(generated.Contains("DeleteBehavior.ClientNoAction"),
        "Expected generated C# to use ClientNoAction for independent entity relationships.");
    AssertCondition(generated.Contains("DeleteBehavior.Restrict"),
        "Expected generated C# to use Restrict for compound relationships.");
    AssertCondition(!generated.Contains("ShuntCompensator"),
        "Did not expect removed ShuntCompensator sample types in the current generated C#.");
}

void VerifyLookupEqualitySemantics()
{
    AssertCondition(new SampleProfile.CrewStatusKind { Name = "arrived" }
        .Equals(new SampleProfile.CrewStatusKind { Name = "arrived" }),
        "Expected lookup types to compare by natural Name key.");
    AssertCondition(new SampleProfile.TelephoneNumber { Id = "tel-1" }
        .Equals(new SampleProfile.TelephoneNumber { Id = "tel-1" }),
        "Expected compound types to compare by surrogate Id.");
    AssertCondition(new SampleProfile.Name { Id = "name-1" }
        .Equals(new SampleProfile.Name { Id = "name-1" }),
        "Expected Name entities to compare by surrogate Id.");
    AssertCondition(!new SampleProfile.Organisation { MRId = "same-id" }
        .Equals(new SampleProfile.ParentOrganization { MRId = "same-id" }),
        "Expected IdentifiedObject equality to reject different runtime types.");
    AssertCondition(!string.IsNullOrWhiteSpace(new SampleProfile.ElectronicAddress().Id),
        "Expected ElectronicAddress constructor to assign a surrogate Id.");
    AssertCondition(!string.IsNullOrWhiteSpace(new SampleProfile.TelephoneNumber().Id),
        "Expected TelephoneNumber constructor to assign a surrogate Id.");
    AssertCondition(!string.IsNullOrWhiteSpace(new SampleProfile.StreetAddress().Id),
        "Expected StreetAddress constructor to assign a surrogate Id.");

    var phone1 = new SampleProfile.TelephoneNumber();
    var phone2 = new SampleProfile.TelephoneNumber();
    AssertCondition(phone1.Id != phone2.Id,
        "Expected each TelephoneNumber constructor call to assign a distinct surrogate Id.");
    var addr1 = new SampleProfile.ElectronicAddress();
    var addr2 = new SampleProfile.ElectronicAddress();
    AssertCondition(addr1.Id != addr2.Id,
        "Expected each ElectronicAddress constructor call to assign a distinct surrogate Id.");
}

void VerifyNameAssociationBehavior()
{
    WithFreshDatabase(connection =>
    {
        using (var context = new SampleProfileDbContext(connection))
        {
            var organisation = new SampleProfile.Organisation
            {
                MRId = "org-name-001",
                NameValue = "Acme Utility",
                Phone1 = new SampleProfile.TelephoneNumber { ItuPhone = "+1-555-0100" }
            };

            var name = new SampleProfile.Name
            {
                Id = "name-001",
                NameValue = "Acme Utility Legal Name",
                IdentifiedObject = organisation
            };

            context.Add(organisation);
            context.Add(name);
            context.SaveChanges();
        }

        using (var context = new SampleProfileDbContext(connection))
        {
            var loaded = context.Names
                .Include(x => x.IdentifiedObject)
                .Single(x => x.Id == "name-001");

            AssertCondition(loaded.IdentifiedObject?.MRId == "org-name-001",
                "Expected Name.IdentifiedObject to round-trip through EF Core.");
            AssertCondition(loaded.NameValue == "Acme Utility Legal Name",
                "Expected Name.NameValue to round-trip through EF Core.");
        }

        using (var context = new SampleProfileDbContext(connection))
        {
            context.Remove(context.Names.Single(x => x.Id == "name-001"));
            context.SaveChanges();
        }

        using (var context = new SampleProfileDbContext(connection))
        {
            AssertCondition(context.Names.Count() == 0, "Expected Name deletion to remove only the Name row.");
            AssertCondition(context.Organisations.Count() == 1, "Expected Organisation to remain after deleting Name.");
            AssertCondition(context.IdentifiedObjects.Count() == 1,
                "Expected IdentifiedObject base row to remain after deleting Name.");
        }

        using (var context = new SampleProfileDbContext(connection))
        {
            context.Add(new SampleProfile.Name
            {
                Id = "name-002",
                NameValue = "Blocking Name",
                IdentifiedObjectId = "org-name-001"
            });
            context.SaveChanges();
        }

        var deleteEx = TryDelete(connection, context => context.Organisations.Single(x => x.MRId == "org-name-001"));
        AssertCondition(deleteEx is DbUpdateException,
            "Expected deleting an Organisation still referenced by Name to fail.");

        using var verificationContext = new SampleProfileDbContext(connection);
        AssertCondition(verificationContext.Organisations.Count() == 1,
            "Expected Organisation row to remain after failed delete.");
        AssertCondition(verificationContext.Names.Count() == 1,
            "Expected Name row to remain after failed Organisation delete.");
    });
}

void VerifyParentOrganisationDeleteGuard()
{
    WithFreshDatabase(connection =>
    {
        using (var context = new SampleProfileDbContext(connection))
        {
            context.Add(new SampleProfile.ParentOrganization
            {
                MRId = "org-parent-001",
                NameValue = "Parent Utility"
            });
            context.Add(new SampleProfile.Organisation
            {
                MRId = "org-child-001",
                NameValue = "Child Utility",
                ParentOrganisationId = "org-parent-001"
            });
            context.SaveChanges();
        }

        var deleteEx = TryDelete(connection, context => context.ParentOrganizations.Single(x => x.MRId == "org-parent-001"));
        AssertCondition(deleteEx is DbUpdateException,
            "Expected deleting ParentOrganization while a child Organisation still references it to fail.");

        using var verificationContext = new SampleProfileDbContext(connection);
        AssertCondition(verificationContext.ParentOrganizations.Count() == 1,
            "Expected ParentOrganization row to remain after failed delete.");
        AssertCondition(verificationContext.Organisations.Count() == 2,
            "Expected child Organisation row to remain after failed parent delete.");
    });
}

void VerifyGeneratedCompoundReplacementCleanup()
{
    WithFreshDatabase(connection =>
    {
        string originalPhoneId;
        string originalStreetAddressId;

        using (var context = new SampleProfileDbContext(connection))
        {
            var organisation = new SampleProfile.Organisation
            {
                MRId = "org-replace-001",
                NameValue = "Replace Utility",
                Phone1 = new SampleProfile.TelephoneNumber { ItuPhone = "+1-555-0200" },
                StreetAddress = CreateAddressGraph("replace-initial")
            };

            context.Add(organisation);
            context.SaveChanges();

            originalPhoneId = organisation.Phone1Id!;
            originalStreetAddressId = organisation.StreetAddressId!;
        }

        using (var context = new SampleProfileDbContext(connection))
        {
            var loaded = context.Organisations
                .Include(x => x.Phone1)
                .Include(x => x.StreetAddress)!.ThenInclude(x => x!.Status)
                .Include(x => x.StreetAddress)!.ThenInclude(x => x!.StreetDetail)
                .Include(x => x.StreetAddress)!.ThenInclude(x => x!.TownDetail)
                .Single(x => x.MRId == "org-replace-001");

            loaded.Phone1 = new SampleProfile.TelephoneNumber { ItuPhone = "+1-555-0299" };
            loaded.StreetAddress = CreateAddressGraph("replace-updated");
            context.SaveChanges();
        }

        using var verificationContext = new SampleProfileDbContext(connection);
        var reloaded = verificationContext.Organisations
            .Include(x => x.Phone1)
            .Include(x => x.StreetAddress)!.ThenInclude(x => x!.Status)
            .Include(x => x.StreetAddress)!.ThenInclude(x => x!.StreetDetail)
            .Include(x => x.StreetAddress)!.ThenInclude(x => x!.TownDetail)
            .Single(x => x.MRId == "org-replace-001");

        AssertCondition(reloaded.Phone1?.ItuPhone == "+1-555-0299",
            "Expected replacing Phone1 to persist the new TelephoneNumber.");
        AssertCondition(reloaded.Phone1Id != originalPhoneId,
            "Expected replacing Phone1 to update the stored foreign key.");
        AssertCondition(reloaded.StreetAddressId != originalStreetAddressId,
            "Expected replacing StreetAddress to update the stored foreign key.");
        AssertCondition(verificationContext.Set<SampleProfile.TelephoneNumber>().Count() == 1,
            "Expected generated cleanup to remove the replaced TelephoneNumber row.");
        AssertCondition(verificationContext.Set<SampleProfile.StreetAddress>().Count() == 1,
            "Expected generated cleanup to remove the replaced StreetAddress row.");
        AssertCondition(verificationContext.Set<SampleProfile.Status>().Count() == 1,
            "Expected Phase 2 generated cleanup to remove the nested Status row orphaned by StreetAddress replacement.");
        AssertCondition(verificationContext.Set<SampleProfile.StreetDetail>().Count() == 1,
            "Expected Phase 2 generated cleanup to remove the nested StreetDetail row orphaned by StreetAddress replacement.");
        AssertCondition(verificationContext.Set<SampleProfile.TownDetail>().Count() == 1,
            "Expected Phase 2 generated cleanup to remove the nested TownDetail row orphaned by StreetAddress replacement.");
    });
}

void VerifyGeneratedNullDetachCleanup()
{
    WithFreshDatabase(connection =>
    {
        using (var context = new SampleProfileDbContext(connection))
        {
            context.Add(new SampleProfile.Organisation
            {
                MRId = "org-null-001",
                NameValue = "Null Detach Utility",
                ElectronicAddress = new SampleProfile.ElectronicAddress { Email1 = "ops@example.com" },
                Phone1 = new SampleProfile.TelephoneNumber { ItuPhone = "+1-555-0300" },
                StreetAddress = CreateAddressGraph("null-detach")
            });
            context.SaveChanges();
        }

        using (var context = new SampleProfileDbContext(connection))
        {
            var loaded = context.Organisations
                .Include(x => x.ElectronicAddress)
                .Include(x => x.Phone1)
                .Include(x => x.StreetAddress)
                .Single(x => x.MRId == "org-null-001");

            loaded.ElectronicAddress = null;
            loaded.ElectronicAddressId = null;
            loaded.Phone1 = null;
            loaded.Phone1Id = null;
            loaded.StreetAddress = null;
            loaded.StreetAddressId = null;
            context.SaveChanges();
        }

        using var verificationContext = new SampleProfileDbContext(connection);
        var reloaded = verificationContext.Organisations.Single(x => x.MRId == "org-null-001");
        AssertCondition(reloaded.ElectronicAddressId is null, "Expected ElectronicAddressId to be cleared.");
        AssertCondition(reloaded.Phone1Id is null, "Expected Phone1Id to be cleared.");
        AssertCondition(reloaded.StreetAddressId is null, "Expected StreetAddressId to be cleared.");
        AssertCondition(verificationContext.Set<SampleProfile.ElectronicAddress>().Count() == 0,
            "Expected generated cleanup to delete detached ElectronicAddress rows.");
        AssertCondition(verificationContext.Set<SampleProfile.TelephoneNumber>().Count() == 0,
            "Expected generated cleanup to delete detached TelephoneNumber rows.");
        AssertCondition(verificationContext.Set<SampleProfile.StreetAddress>().Count() == 0,
            "Expected generated cleanup to delete detached StreetAddress rows.");
        AssertCondition(verificationContext.Set<SampleProfile.Status>().Count() == 0,
            "Expected Phase 2 generated cleanup to delete nested Status rows orphaned by StreetAddress null-detach.");
        AssertCondition(verificationContext.Set<SampleProfile.StreetDetail>().Count() == 0,
            "Expected Phase 2 generated cleanup to delete nested StreetDetail rows orphaned by StreetAddress null-detach.");
        AssertCondition(verificationContext.Set<SampleProfile.TownDetail>().Count() == 0,
            "Expected Phase 2 generated cleanup to delete nested TownDetail rows orphaned by StreetAddress null-detach.");
    });
}

void VerifyGeneratedMappingBaseline()
{
    WithFreshGeneratedOnlyDatabase(connection =>
    {
        using (var context = new GeneratedOnlySampleProfileDbContext(connection))
        {
            context.Add(new SampleProfile.Organisation
            {
                MRId = "org-baseline-001",
                NameValue = "Generated Baseline Utility",
                Phone1 = new SampleProfile.TelephoneNumber { ItuPhone = "+1-555-0400" },
                StreetAddress = CreateAddressGraph("baseline-initial")
            });
            context.SaveChanges();
        }

        using (var context = new GeneratedOnlySampleProfileDbContext(connection))
        {
            var loaded = context.Organisations
                .Include(x => x.Phone1)
                .Include(x => x.StreetAddress)!.ThenInclude(x => x!.Status)
                .Include(x => x.StreetAddress)!.ThenInclude(x => x!.StreetDetail)
                .Include(x => x.StreetAddress)!.ThenInclude(x => x!.TownDetail)
                .Single(x => x.MRId == "org-baseline-001");

            loaded.Phone1 = new SampleProfile.TelephoneNumber { ItuPhone = "+1-555-0499" };
            loaded.StreetAddress = CreateAddressGraph("baseline-updated");
            context.SaveChanges();
        }

        using (var context = new GeneratedOnlySampleProfileDbContext(connection))
        {
            var loaded = context.Organisations
                .Include(x => x.Phone1)
                .Single(x => x.MRId == "org-baseline-001");

            loaded.Phone1 = null;
            loaded.Phone1Id = null;
            context.SaveChanges();
        }

        using var verificationContext = new GeneratedOnlySampleProfileDbContext(connection);
        AssertCondition(verificationContext.Set<SampleProfile.TelephoneNumber>().Count() == 2,
            "Expected generated-only baseline to leave replaced and detached TelephoneNumber rows orphaned.");
        AssertCondition(verificationContext.Set<SampleProfile.StreetAddress>().Count() == 2,
            "Expected generated-only baseline to leave replaced StreetAddress rows orphaned.");
        AssertCondition(verificationContext.Set<SampleProfile.Status>().Count() == 2,
            "Expected generated-only baseline to leave nested Status rows orphaned.");
        AssertCondition(verificationContext.Set<SampleProfile.StreetDetail>().Count() == 2,
            "Expected generated-only baseline to leave nested StreetDetail rows orphaned.");
        AssertCondition(verificationContext.Set<SampleProfile.TownDetail>().Count() == 2,
            "Expected generated-only baseline to leave nested TownDetail rows orphaned.");
    });

    diagnostics.Add("BASELINE OBSERVED: without the generated DbContextBase SaveChanges cleanup helper, replaced and detached compound graphs remain orphaned.");
}

void VerifyInheritanceStorage()
{
    WithFreshDatabase(connection =>
    {
        using (var context = new SampleProfileDbContext(connection))
        {
            context.Add(new SampleProfile.Organisation
            {
                MRId = "org-base-001",
                NameValue = "Base Organisation"
            });
            context.Add(new SampleProfile.ParentOrganization
            {
                MRId = "org-parent-002",
                NameValue = "Derived Parent Organisation"
            });
            context.Add(new SampleProfile.WireInfo
            {
                MRId = "wire-base-001",
                NameValue = "Base Wire"
            });
            context.Add(new SampleProfile.OverheadWireInfo
            {
                MRId = "wire-overhead-001",
                NameValue = "Derived Wire"
            });
            context.SaveChanges();
        }

        using var verificationContext = new SampleProfileDbContext(connection);
        var identifiedObjects = verificationContext.IdentifiedObjects.OrderBy(x => x.MRId).ToList();
        AssertCondition(identifiedObjects.Count == 4,
            "Expected four IdentifiedObject rows across the inheritance hierarchy.");
        AssertCondition(identifiedObjects.Any(x => x.GetType() == typeof(SampleProfile.Organisation)),
            "Expected base-set query to materialize Organisation.");
        AssertCondition(identifiedObjects.Any(x => x.GetType() == typeof(SampleProfile.ParentOrganization)),
            "Expected base-set query to materialize ParentOrganization.");
        AssertCondition(identifiedObjects.Any(x => x.GetType() == typeof(SampleProfile.WireInfo)),
            "Expected base-set query to materialize WireInfo.");
        AssertCondition(identifiedObjects.Any(x => x.GetType() == typeof(SampleProfile.OverheadWireInfo)),
            "Expected base-set query to materialize OverheadWireInfo.");
    });
}

void VerifyInheritanceDeleteCleanup()
{
    WithFreshDatabase(connection =>
    {
        using (var context = new SampleProfileDbContext(connection))
        {
            context.Add(new SampleProfile.ParentOrganization
            {
                MRId = "org-parent-delete-001",
                NameValue = "Delete Parent"
            });
            context.Add(new SampleProfile.OverheadWireInfo
            {
                MRId = "wire-overhead-delete-001",
                NameValue = "Delete Wire"
            });
            context.SaveChanges();
        }

        using (var context = new SampleProfileDbContext(connection))
        {
            context.Remove(context.ParentOrganizations.Single(x => x.MRId == "org-parent-delete-001"));
            context.Remove(context.OverheadWireInfos.Single(x => x.MRId == "wire-overhead-delete-001"));
            context.SaveChanges();
        }

        using var verificationContext = new SampleProfileDbContext(connection);
        AssertCondition(!verificationContext.IdentifiedObjects.Any(x => x.MRId == "org-parent-delete-001"),
            "Expected deleting ParentOrganization to remove its IdentifiedObject base row.");
        AssertCondition(!verificationContext.Organisations.Any(x => x.MRId == "org-parent-delete-001"),
            "Expected deleting ParentOrganization to remove its Organisation row.");
        AssertCondition(!verificationContext.ParentOrganizations.Any(x => x.MRId == "org-parent-delete-001"),
            "Expected deleting ParentOrganization to remove its derived row.");

        AssertCondition(!verificationContext.IdentifiedObjects.Any(x => x.MRId == "wire-overhead-delete-001"),
            "Expected deleting OverheadWireInfo to remove its IdentifiedObject base row.");
        AssertCondition(!verificationContext.AssetInfos.Any(x => x.MRId == "wire-overhead-delete-001"),
            "Expected deleting OverheadWireInfo to remove its AssetInfo intermediate row.");
        AssertCondition(!verificationContext.WireInfos.Any(x => x.MRId == "wire-overhead-delete-001"),
            "Expected deleting OverheadWireInfo to remove its WireInfo row.");
        AssertCondition(!verificationContext.OverheadWireInfos.Any(x => x.MRId == "wire-overhead-delete-001"),
            "Expected deleting OverheadWireInfo to remove its derived row.");
    });
}

async Task VerifyGeneratedCompoundAsyncCleanup()
{
    await WithFreshDatabaseAsync(async connection =>
    {
        string originalPhoneId;

        using (var context = new SampleProfileDbContext(connection))
        {
            var organisation = new SampleProfile.Organisation
            {
                MRId = "org-async-001",
                NameValue = "Async Utility",
                Phone1 = new SampleProfile.TelephoneNumber { ItuPhone = "+1-555-0500" },
                StreetAddress = CreateAddressGraph("async-initial")
            };

            context.Add(organisation);
            await context.SaveChangesAsync();

            originalPhoneId = organisation.Phone1Id!;
        }

        using (var context = new SampleProfileDbContext(connection))
        {
            var loaded = context.Organisations
                .Include(x => x.Phone1)
                .Include(x => x.StreetAddress)!.ThenInclude(x => x!.Status)
                .Include(x => x.StreetAddress)!.ThenInclude(x => x!.StreetDetail)
                .Include(x => x.StreetAddress)!.ThenInclude(x => x!.TownDetail)
                .Single(x => x.MRId == "org-async-001");

            loaded.Phone1 = new SampleProfile.TelephoneNumber { ItuPhone = "+1-555-0599" };
            loaded.StreetAddress = CreateAddressGraph("async-updated");
            await context.SaveChangesAsync();
        }

        using var verificationContext = new SampleProfileDbContext(connection);
        AssertCondition(verificationContext.Set<SampleProfile.TelephoneNumber>().Count() == 1,
            "Expected SaveChangesAsync cleanup to remove the replaced TelephoneNumber row.");
        AssertCondition(verificationContext.Set<SampleProfile.StreetAddress>().Count() == 1,
            "Expected SaveChangesAsync cleanup to remove the replaced StreetAddress row.");
        AssertCondition(verificationContext.Set<SampleProfile.Status>().Count() == 1,
            "Expected SaveChangesAsync Phase 2 cleanup to remove nested Status rows.");
        AssertCondition(verificationContext.Set<SampleProfile.StreetDetail>().Count() == 1,
            "Expected SaveChangesAsync Phase 2 cleanup to remove nested StreetDetail rows.");
        AssertCondition(verificationContext.Set<SampleProfile.TownDetail>().Count() == 1,
            "Expected SaveChangesAsync Phase 2 cleanup to remove nested TownDetail rows.");
    });
}

void VerifyCompoundlessEntityCleanup()
{
    WithFreshDatabase(connection =>
    {
        // Exercises the early-return path in the generated SaveChanges overrides:
        // when CollectCompoundOrphans returns an empty list, the second SaveChanges
        // is skipped and rows = base.SaveChanges() is returned directly.
        using (var context = new SampleProfileDbContext(connection))
        {
            context.Add(new SampleProfile.Organisation
            {
                MRId = "org-compoundless-001",
                NameValue = "No Compounds Utility"
            });
            context.SaveChanges();
        }

        using (var context = new SampleProfileDbContext(connection))
        {
            var loaded = context.Organisations.Single(x => x.MRId == "org-compoundless-001");
            loaded.NameValue = "No Compounds Utility Updated";
            context.SaveChanges();
        }

        using (var context = new SampleProfileDbContext(connection))
        {
            context.Remove(context.Organisations.Single(x => x.MRId == "org-compoundless-001"));
            context.SaveChanges();
        }

        using var verificationContext = new SampleProfileDbContext(connection);
        AssertCondition(!verificationContext.Organisations.Any(x => x.MRId == "org-compoundless-001"),
            "Expected compoundless Organisation to be deleted cleanly.");
        AssertCondition(!verificationContext.IdentifiedObjects.Any(x => x.MRId == "org-compoundless-001"),
            "Expected compoundless Organisation deletion to remove its IdentifiedObject base row.");
    });
}

async Task WithFreshDatabaseAsync(Func<SqliteConnection, Task> action)
{
    using var connection = new SqliteConnection("Data Source=:memory:");
    connection.Open();

    using (var setupContext = new SampleProfileDbContext(connection))
    {
        setupContext.Database.EnsureDeleted();
        setupContext.Database.EnsureCreated();
    }

    await action(connection);
}


void WithFreshDatabase(Action<SqliteConnection> action)
{
    using var connection = new SqliteConnection("Data Source=:memory:");
    connection.Open();

    using (var setupContext = new SampleProfileDbContext(connection))
    {
        setupContext.Database.EnsureDeleted();
        setupContext.Database.EnsureCreated();
    }

    action(connection);
}

void WithFreshGeneratedOnlyDatabase(Action<SqliteConnection> action)
{
    using var connection = new SqliteConnection("Data Source=:memory:");
    connection.Open();

    using (var setupContext = new GeneratedOnlySampleProfileDbContext(connection))
    {
        setupContext.Database.EnsureDeleted();
        setupContext.Database.EnsureCreated();
    }

    action(connection);
}

SampleProfile.StreetAddress CreateAddressGraph(string prefix)
{
    return new SampleProfile.StreetAddress
    {
        PoBox = $"{prefix}-po-box",
        StreetDetail = new SampleProfile.StreetDetail { NameValue = $"{prefix}-street" },
        TownDetail = new SampleProfile.TownDetail { NameValue = $"{prefix}-town" },
        Status = new SampleProfile.Status()
    };
}

Exception? TryDelete(SqliteConnection connection, Func<SampleProfileDbContext, object> targetSelector)
{
    using var context = new SampleProfileDbContext(connection);
    context.Remove(targetSelector(context));

    try
    {
        context.SaveChanges();
        return null;
    }
    catch (Exception ex)
    {
        return ex;
    }
}

void AssertCondition(bool condition, string message)
{
    if (!condition)
    {
        throw new InvalidOperationException(message);
    }
}

void AssertPropertyKey(Type type, string propertyName, string columnName, int maxLength)
{
    var property = type.GetProperty(propertyName, BindingFlags.Instance | BindingFlags.Public | BindingFlags.DeclaredOnly)
        ?? throw new InvalidOperationException($"Could not find declared property {type.Name}.{propertyName}.");

    AssertCondition(property.GetCustomAttribute<KeyAttribute>() is not null,
        $"Expected {type.Name}.{propertyName} to carry [Key].");

    var column = property.GetCustomAttribute<ColumnAttribute>();
    AssertCondition(column?.Name == columnName,
        $"Expected {type.Name}.{propertyName} to map to column '{columnName}'.");

    var length = property.GetCustomAttribute<MaxLengthAttribute>();
    AssertCondition(length?.Length == maxLength,
        $"Expected {type.Name}.{propertyName} to carry [MaxLength({maxLength})].");
}

void AssertNaturalKey(Type type, string propertyName, string columnName, int maxLength)
{
    AssertPropertyKey(type, propertyName, columnName, maxLength);
}

void AssertNoDeclaredKeyProperties(Type type)
{
    var keyedProperties = type
        .GetProperties(BindingFlags.Instance | BindingFlags.Public | BindingFlags.DeclaredOnly)
        .Where(p => p.GetCustomAttribute<KeyAttribute>() is not null)
        .Select(p => p.Name)
        .ToList();

    AssertCondition(keyedProperties.Count == 0,
        $"Did not expect {type.Name} to declare [Key] on its own properties ({string.Join(", ", keyedProperties)}).");
}

bool HasSinglePropertyUniqueIndex(IEnumerable<IndexAttribute> indexes, string propertyName)
{
    return indexes.Any(index => index.IsUnique && index.PropertyNames.Count == 1 && index.PropertyNames[0] == propertyName);
}

void AssertPrimaryKey(SampleProfileDbContext context, Type type, string propertyName)
{
    var key = GetEntityType(context, type).FindPrimaryKey()
        ?? throw new InvalidOperationException($"Expected primary key metadata for {type.Name}.");

    AssertCondition(key.Properties.Count == 1 && key.Properties[0].Name == propertyName,
        $"Expected {type.Name} primary key to be {propertyName}.");
}

void AssertTableName(SampleProfileDbContext context, Type type, string tableName)
{
    var entityType = GetEntityType(context, type);
    AssertCondition(entityType.GetTableName() == tableName,
        $"Expected EF table name for {type.Name} to be {tableName}.");
}

void AssertColumnName(SampleProfileDbContext context, Type type, string propertyName, string expectedColumnName)
{
    var entityType = GetEntityType(context, type);
    var property = entityType.FindProperty(propertyName)
        ?? throw new InvalidOperationException($"Could not find EF property metadata for {type.Name}.{propertyName}.");

    var table = StoreObjectIdentifier.Table(entityType.GetTableName()!, entityType.GetSchema());
    AssertCondition(property.GetColumnName(table) == expectedColumnName,
        $"Expected EF column name for {type.Name}.{propertyName} to be {expectedColumnName}.");
}

void AssertDeleteBehavior(SampleProfileDbContext context, Type type, string propertyName, DeleteBehavior expectedBehavior)
{
    var foreignKey = GetEntityType(context, type).GetForeignKeys()
        .SingleOrDefault(fk => fk.Properties.Count == 1 && fk.Properties[0].Name == propertyName)
        ?? throw new InvalidOperationException($"Could not find foreign key metadata for {type.Name}.{propertyName}.");

    AssertCondition(foreignKey.DeleteBehavior == expectedBehavior,
        $"Expected delete behavior for {type.Name}.{propertyName} to be {expectedBehavior}, not {foreignKey.DeleteBehavior}.");
}

void AssertUniqueIndex(SampleProfileDbContext context, Type type, string propertyName)
{
    var index = GetEntityType(context, type).GetIndexes()
        .SingleOrDefault(i => i.Properties.Count == 1 && i.Properties[0].Name == propertyName)
        ?? throw new InvalidOperationException($"Could not find index metadata for {type.Name}.{propertyName}.");

    AssertCondition(index.IsUnique,
        $"Expected index for {type.Name}.{propertyName} to be unique.");
}

void AssertForeignKeyTarget(SampleProfileDbContext context, Type type, string propertyName, Type principalType, string principalPropertyName)
{
    var foreignKey = GetEntityType(context, type).GetForeignKeys()
        .SingleOrDefault(fk => fk.Properties.Count == 1 && fk.Properties[0].Name == propertyName)
        ?? throw new InvalidOperationException($"Could not find foreign key metadata for {type.Name}.{propertyName}.");

    AssertCondition(foreignKey.PrincipalEntityType.ClrType == principalType,
        $"Expected {type.Name}.{propertyName} to reference {principalType.Name}.");
    AssertCondition(foreignKey.PrincipalKey.Properties.Count == 1 && foreignKey.PrincipalKey.Properties[0].Name == principalPropertyName,
        $"Expected {type.Name}.{propertyName} to target {principalType.Name}.{principalPropertyName}.");
}

IEntityType GetEntityType(SampleProfileDbContext context, Type type)
{
    return context.Model.FindEntityType(type)
        ?? throw new InvalidOperationException($"Could not find entity type metadata for {type.Name}.");
}


