import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {HeroesComponent} from './heroes/heroes.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import {HeroDetailComponent} from './hero-detail/hero-detail.component';

// 路由配置
// path: 用来匹配浏览器地址栏中 URL 的字符串。
// component: 导航到该路由时，路由器应该创建的组件。
// 这会告诉路由器把该 URL 与 path：'heroes' 匹配。
// 如果网址类似于 localhost:4200/heroes 就显示 HeroesComponent
const routes: Routes = [
  {path: 'heroes', component: HeroesComponent},
  { path: 'dashboard', component: DashboardComponent },
  // 这个路由会把一个与空路径“完全匹配(full)”的 URL 重定向到路径为 '/dashboard' 的路由。
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'detail/:id', component: HeroDetailComponent },
];
// @NgModule 元数据会初始化路由器，并开始监听浏览器地址的变化。
@NgModule({
  // 用routes配置RouterModule
  // 这个方法之所以叫 forRoot()，是因为你要在应用的顶层配置这个路由器。
  // forRoot() 方法会提供路由所需的服务提供者和指令，还会基于浏览器的当前 URL 执行首次导航。
  imports: [RouterModule.forRoot(routes)],
  // 接下来，AppRoutingModule 导出 RouterModule，以便它在整个应用程序中生效。
  exports: [RouterModule]
})
export class AppRoutingModule { }
