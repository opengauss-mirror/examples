using DotNetLogin.Services;
using DotNetLogin.ViewModels;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Mvc;
using System.Collections.Generic;
using System.Security.Claims;
using System.Threading.Tasks;

namespace DotNetLogin.Controllers
{
   public class AccountController : Controller
   {
      private readonly IUserService userService;

      public AccountController(IUserService userService)
      {
         this.userService = userService;
      }

      [HttpGet]
      public IActionResult Login(string returnUrl = null)
      {
         return View(new LoginViewModel 
         {
            ReturnUrl = returnUrl
         });
      }

      private bool ValidateLogin(string username, string password)
      {
         return this.userService.ValidateUser(username, password);
      }

      [HttpPost]
      public async Task<IActionResult> Login(LoginViewModel loginViewModel)
      {
         if (!this.ModelState.IsValid)
         {
            return View(loginViewModel);
         }

         loginViewModel.Username = loginViewModel.Username?.Trim();
         loginViewModel.Password = loginViewModel.Password?.Trim();
         loginViewModel.IsError = false;

         if (ValidateLogin(loginViewModel.Username, loginViewModel.Password))
         {
            var claims = new List<Claim>
            {
               new Claim("user", loginViewModel.Username),
            };

            await HttpContext.SignInAsync(new ClaimsPrincipal(new ClaimsIdentity(claims, "Cookies", "user", "role")));

            if (Url.IsLocalUrl(loginViewModel.ReturnUrl))
            {
               return Redirect(loginViewModel.ReturnUrl);
            }
            else
            {
               return Redirect("/");
            }
         }

         loginViewModel.IsError = true;

         return View(loginViewModel);
      }


      public async Task<IActionResult> Logout()
      {
         await HttpContext.SignOutAsync();

         return Redirect("/");
      }
   }
}
