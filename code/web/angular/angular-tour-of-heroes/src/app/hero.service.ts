import { Injectable } from '@angular/core';
import { Hero } from './hero';
import { HEROES } from './mock-heroes';
import { Observable, of } from 'rxjs';
import { MessageService}     from './message.service';

@Injectable({
  providedIn: 'root'   //使用root注册器把当前类，也就是HeroSerivce注入到依赖注入系统中
})
export class HeroService {

  getHeroes(): Observable<Hero[]> {
    //获取一条数据时，发送一条消息
    this.messageService.add('HeroService: fetched heroes');
    //获取消息
    return of(HEROES);   //模拟从远端获取数据，返回一个Observable对象
  }

  // 注入messageService
  constructor(private messageService:MessageService) { }
}
