using DotNetLogin.Models;

namespace DotNetLogin.Services
{
   public interface IUserService
   {
      UserInfo GetUser(string username);

      bool ValidateUser(string username,string  password);
   }
}
