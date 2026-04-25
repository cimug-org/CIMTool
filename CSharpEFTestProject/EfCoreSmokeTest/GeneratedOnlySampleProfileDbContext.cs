using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

public sealed class GeneratedOnlySampleProfileDbContext : DbContext
{
    private readonly SqliteConnection _connection;

    public DbSet<SampleProfile.IdentifiedObject> IdentifiedObjects => Set<SampleProfile.IdentifiedObject>();
    public DbSet<SampleProfile.Name> Names => Set<SampleProfile.Name>();
    public DbSet<SampleProfile.Organisation> Organisations => Set<SampleProfile.Organisation>();
    public DbSet<SampleProfile.ParentOrganization> ParentOrganizations => Set<SampleProfile.ParentOrganization>();
    public DbSet<SampleProfile.WireInfo> WireInfos => Set<SampleProfile.WireInfo>();
    public DbSet<SampleProfile.OverheadWireInfo> OverheadWireInfos => Set<SampleProfile.OverheadWireInfo>();

    public GeneratedOnlySampleProfileDbContext(SqliteConnection connection)
    {
        _connection = connection;
    }

    protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
    {
        if (!optionsBuilder.IsConfigured)
        {
            optionsBuilder.UseSqlite(_connection);
            optionsBuilder.EnableSensitiveDataLogging();
        }
    }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        SampleProfile.ModelConfiguration.ConfigureModel(modelBuilder);
    }
}
