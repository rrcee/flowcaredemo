import {type RefObject,useEffect,useRef} from 'react';
export function useMousePositionRef(containerRef:RefObject<HTMLElement|null>) {
 const position=useRef({x:0,y:0});
 useEffect(()=>{
  const move=(e:PointerEvent)=>{const rect=containerRef.current?.getBoundingClientRect();if(!rect||e.pointerType==='touch')return;
   const inside=e.clientX>=rect.left&&e.clientX<=rect.right&&e.clientY>=rect.top&&e.clientY<=rect.bottom;
   position.current=inside?{x:(e.clientX-rect.left-rect.width/2)/(rect.width/2),y:(e.clientY-rect.top-rect.height/2)/(rect.height/2)}:{x:0,y:0};};
  const reset=()=>{position.current={x:0,y:0};};
  window.addEventListener('pointermove',move,{passive:true});window.addEventListener('blur',reset);
  return()=>{window.removeEventListener('pointermove',move);window.removeEventListener('blur',reset);};
 },[containerRef]);return position;
}
