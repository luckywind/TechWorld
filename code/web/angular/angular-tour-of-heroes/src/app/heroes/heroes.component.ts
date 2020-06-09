import { Component, OnInit } from '@angular/core';
import { Hero } from '../hero';
// import { HEROES }  from '../mock-heroes';
import { HeroService } from '../hero.service';
import { MessageService } from '../message.service';

@Component({
  selector: 'app-heroes',
  templateUrl: './heroes.component.html',
  styleUrls: ['./heroes.component.css']
})
export class HeroesComponent implements OnInit {
  // hero :Hero={
  //   id:1,
  //   name:'Windstorm'
  // };
  selectedHero: Hero;
  onSelect(hero: Hero): void {
    this.selectedHero = hero;
    //添加一条消息
    this.messageService.add(`HeroService: Selected hero id=${hero.id}`);
  }

  // heroes = HEROES;
   heroes :Hero[];
   //构造器中注入 HeroService
  //  
  constructor(private heroService:HeroService, private messageService:MessageService ) { }

  getHeroes(): void {
    // this.heroes = this.heroService.getHeroes();  //调用服务提供者的方法


    //等待 Observable 发出这个英雄数组，这可能立即发生，也可能会在几分钟之后
    this.heroService.getHeroes()                  //Observable对象
    .subscribe(heroes => this.heroes = heroes);
  }



  ngOnInit(): void {
    //在声明周期钩子函数ngOnInit函数中调用初始化方法，当然也可以在构造函数中调用，只是不推荐
    this.getHeroes();
  }

}
