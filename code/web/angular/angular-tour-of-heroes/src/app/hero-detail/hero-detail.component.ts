import { Component, OnInit ,Input} from '@angular/core';
import { Hero } from '../hero';

@Component({
  selector: 'app-hero-detail',
  templateUrl: './hero-detail.component.html',
  styleUrls: ['./hero-detail.component.css']
})
export class HeroDetailComponent implements OnInit {
  // 这个用@Input注解的hero属性，使得外部的HeroesComponent组件可以绑定它，例如：
  // <app-hero-detail [hero]="selectedHero"></app-hero-detail>
  @Input() hero: Hero;
  constructor() { }

  ngOnInit(): void {
  }

}
