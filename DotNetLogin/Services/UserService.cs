using Dapper;
using DotNetLogin.Models;
using DotNetLogin.Utils;
using System;
using System.Linq;

namespace DotNetLogin.Services
{
   public class UserService : IUserService
   {
      private readonly ConnectionFactory connectionFactory;

      public UserService(ConnectionFactory connectionFactory) 
      {
         this.connectionFactory = connectionFactory;
      }

      public UserInfo GetUser(string username)
      {
         if (string.IsNullOrWhiteSpace(username) )
         {
            throw new ArgumentNullException("username");
         }

         using (var connection = this.connectionFactory.CreateConnection()) 
         {
            return connection.Query<UserInfo>("SELECT * FROM user_info WHERE username = @username",new { username }).SingleOrDefault();
         }
      }

      public bool ValidateUser(string username, string password)
      {
         if (string.IsNullOrWhiteSpace(username))
         {
            throw new ArgumentNullException("username");
         }
         if (string.IsNullOrWhiteSpace(password))
         {
            throw new ArgumentNullException("password");
         }

         var userInfo = this.GetUser(username);

         if (userInfo == null) {
            return false;
         }

         //TOOD: 测试临时使用明文
         return password == userInfo.Password;
      }
   }
}
