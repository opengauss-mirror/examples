using DotNetLogin.Services;
using DotNetLogin.Utils;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace DotNetLogin
{
   public class Startup
   {
      public const string CookieScheme = "Cookies";

      public Startup(IConfiguration configuration)
      {
         Configuration = configuration;
      }

      public IConfiguration Configuration { get; }

      public void ConfigureServices(IServiceCollection services)
      {
         //添加认证
         services.AddAuthentication(CookieScheme).AddCookie(CookieScheme, options =>
         {
            options.LoginPath = "/account/login";
         });

         services.AddControllersWithViews();

         //添加业务相关到容器
         services.AddSingleton<ConnectionFactory>();
         services.AddScoped<IUserService, UserService>();
      }

      public void Configure(IApplicationBuilder app, IWebHostEnvironment env)
      {
         if (env.IsDevelopment())
         {
            app.UseDeveloperExceptionPage();
         }
         else
         {
            app.UseExceptionHandler("/Home/Error");
         }

         app.UseStaticFiles();

         app.UseRouting();

         //使用认证
         app.UseAuthentication();
         app.UseAuthorization();

         app.UseEndpoints(endpoints =>
         {
            endpoints.MapControllerRoute(
                   name: "default",
                   pattern: "{controller=Home}/{action=Index}/{id?}");
         });
      }
   }
}
