'use client';
import {createContext,type ReactNode,useCallback,useContext,useEffect,useId,useMemo,useRef} from 'react';
import {useAnimationFrame,useReducedMotion} from 'motion/react';
import {cn} from '@/lib/utils';
import {useMousePositionRef} from '@/hooks/use-mouse-position-ref';
type Entry={element:HTMLDivElement,depth:number,x:number,y:number};
const FloatingContext=createContext<{registerElement:(id:string,el:HTMLDivElement,depth:number)=>void,unregisterElement:(id:string)=>void}|null>(null);
export default function Floating({children,className,sensitivity=1,easingFactor=.045,paused=false}:{children:ReactNode,className?:string,sensitivity?:number,easingFactor?:number,paused?:boolean}) {
 const ref=useRef<HTMLDivElement>(null);const elements=useRef(new Map<string,Entry>());const mouse=useMousePositionRef(ref);const reduced=useReducedMotion();
 const registerElement=useCallback((id:string,element:HTMLDivElement,depth:number)=>{elements.current.set(id,{element,depth,x:0,y:0});},[]);
 const unregisterElement=useCallback((id:string)=>{elements.current.delete(id);},[]);
 const context=useMemo(()=>({registerElement,unregisterElement}),[registerElement,unregisterElement]);
 useAnimationFrame(()=>{elements.current.forEach(e=>{
  const tx=reduced||paused?0:mouse.current.x*e.depth*sensitivity*18;const ty=reduced||paused?0:mouse.current.y*e.depth*sensitivity*14;
  e.x+=(tx-e.x)*easingFactor;e.y+=(ty-e.y)*easingFactor;
  e.element.style.transform=`translate3d(${e.x}px,${e.y}px,0)`;
 });});
 return <FloatingContext.Provider value={context}><div ref={ref} className={cn('floating',className)} aria-hidden="true">{children}</div></FloatingContext.Provider>;
}
export function FloatingElement({children,className,depth=1}:{children:ReactNode,className?:string,depth?:number}){
 const ref=useRef<HTMLDivElement>(null);const id=useId();const context=useContext(FloatingContext);
 useEffect(()=>{if(!ref.current||!context)return;context.registerElement(id,ref.current,depth);return()=>context.unregisterElement(id);},[context,id,depth]);
 return <div ref={ref} className={cn('floating-element',className)}>{children}</div>;
}
