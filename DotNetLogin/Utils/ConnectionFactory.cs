using Dapper;
using Microsoft.Extensions.Configuration;
using Npgsql;
using System;
using System.Data.Common;

namespace DotNetLogin.Utils
{
   public sealed class ConnectionFactory
   {
      private readonly IConfiguration configuration;

      public ConnectionFactory(IConfiguration configuration)
      {
         this.configuration = configuration;
      }

      public DbConnection CreateConnection()
      {
         var connectionString = this.configuration.GetConnectionString("OpenGauss");

         if (string.IsNullOrWhiteSpace(connectionString))
         {
            throw new ArgumentNullException("connectionString OpenGauss is null");
         }

         return new NpgsqlConnection(connectionString);
      }
   }
}
