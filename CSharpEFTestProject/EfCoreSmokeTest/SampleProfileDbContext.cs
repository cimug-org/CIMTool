using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

public sealed class SampleProfileDbContext : SampleProfile.DbContextBase
{
    public SampleProfileDbContext(SqliteConnection connection)
        : base(new DbContextOptionsBuilder().UseSqlite(connection).EnableSensitiveDataLogging().Options)
    {
    }
}
