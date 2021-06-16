using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Threading.Tasks;

namespace DotNetLogin.ViewModels
{
   public class LoginViewModel
   {
      [Required(AllowEmptyStrings = false)]
      public string Username { get; set; }

      [Required(AllowEmptyStrings = false)]
      public string Password { get; set; }

      public string ReturnUrl { get; set; }

      public bool IsError { get; set; }
   }
}
