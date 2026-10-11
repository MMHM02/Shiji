import React from "react";
import {AbsoluteFill, Composition, Easing, interpolate, useCurrentFrame, useVideoConfig} from "remotion";

const green = "#214A37";
const muted = "#778271";
const ease = {extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.22, 1, 0.36, 1)} as const;

export const ShiJiIntro: React.FC = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const t = frame / fps;
  const reveal = interpolate(t, [1.4, 3.1], [0, 1], ease);
  const reset = interpolate(t, [5.2, 5.95], [0, 1], ease);
  const progress = reveal * (1 - reset);
  const orbit = Math.sin(t * Math.PI / 3) * 4;
  return <AbsoluteFill style={{backgroundColor: "#F3F3E9", color: green, fontFamily: '"Microsoft YaHei", sans-serif', overflow: "hidden"}}>
    <div style={{position: "absolute", left: 48, top: 34, fontSize: 13, letterSpacing: 4}}>SHIJI / 每一餐，都值得被记住</div>
    <div style={{position: "absolute", right: 48, top: 34, color: muted, fontSize: 12}}>OPEN SOURCE · ANDROID</div>
    <div style={{position: "absolute", left: 48, top: 127, opacity: 1 - interpolate(progress, [0, 0.35], [0, 1], ease)}}>
      <div style={{fontSize: 60, fontWeight: 600, letterSpacing: -2}}>记下这一餐。</div>
      <div style={{fontSize: 21, marginTop: 21, color: muted}}>拍照、文字，或亲手记录。</div>
    </div>
    <div style={{position: "absolute", left: 48, top: 127, opacity: interpolate(progress, [0.45, 0.9], [0, 1], ease), translate: `0px ${16 * (1 - progress)}px`}}>
      <div style={{fontSize: 60, fontWeight: 600, letterSpacing: -2}}>看懂每一天。</div>
      <div style={{fontSize: 21, marginTop: 21, color: muted}}>从一餐的营养，到日常的习惯。</div>
    </div>
    <div style={{position: "absolute", left: 49, top: 294, width: 356, height: 1, backgroundColor: "#D5DDCE"}}/>
    <div style={{position: "absolute", left: 49, top: 317, display: "flex", alignItems: "center", gap: 14}}>
      <div style={{width: 37, height: 37, borderRadius: 12, backgroundColor: green, color: "#F3F3E9", display: "grid", placeItems: "center", fontSize: 21}}>食</div>
      <span style={{fontSize: 25, fontWeight: 600}}>食记</span><span style={{fontSize: 17, color: muted, letterSpacing: 3}}>SHIJI</span>
    </div>
    <svg width="440" height="350" viewBox="0 0 440 350" style={{position: "absolute", left: 480, top: 69, translate: `0px ${orbit}px`}}>
      <circle cx="218" cy="166" r="150" fill="#E8ECDF"/>
      <circle cx="218" cy="166" r="128" fill="#FCFCF6" stroke="#D5DDCE"/>
      <circle cx="218" cy="166" r="109" fill="none" stroke="#E2E8D9" strokeWidth="9"/>
      <circle cx="218" cy="166" r="109" fill="none" stroke="#567C59" strokeWidth="9" strokeLinecap="round" strokeDasharray={`${685 * progress * 0.76} 685`} transform="rotate(-90 218 166)" opacity={progress}/>
      <g opacity={1-interpolate(progress, [0, 0.45], [0, 1], ease)}>
        <path d="M160 164Q122 94 189 96Q233 127 196 171Q171 187 160 164" fill="#A9BD7F"/>
        <path d="M165 112L190 158M169 141L184 132" stroke="#6C885A" strokeWidth="3" fill="none"/>
        <path d="M223 163Q231 104 272 124Q311 158 270 191Q238 196 223 163" fill="#D3A469"/>
        <path d="M170 205Q210 177 243 207Q264 238 220 254Q177 258 170 230Z" fill="#E1E3C6"/>
        <path d="M187 218L192 229M205 208L212 222M224 226L231 237" stroke="#BDC6A0" strokeWidth="3" strokeLinecap="round"/>
      </g>
      <g opacity={interpolate(progress, [0.45, 0.9], [0, 1], ease)} fill={green} textAnchor="middle">
        <text x="218" y="157" fontSize="49" fontFamily="Microsoft YaHei" fontWeight="600">{Math.round(480 * progress)}</text>
        <text x="218" y="184" fontSize="13" letterSpacing="3">KCAL / 这一餐</text>
        <path d="M201 209L213 221L237 196" fill="none" stroke="#567C59" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
      </g>
    </svg>
    <div style={{position:"absolute", right: 45, bottom: 73, display: "flex", gap: 10, opacity: progress, translate: `0px ${12 * (1-progress)}px`}}>
      {[{name:"蛋白质",value:"28 g",color:"#71885D"},{name:"碳水",value:"56 g",color:"#BA985F"},{name:"脂肪",value:"16 g",color:"#7C968A"}].map(item => <div key={item.name} style={{backgroundColor:"#FCFCF6", border:"1px solid #D9E0D1", borderRadius:12, padding:"11px 17px", minWidth:95}}>
        <div style={{fontSize:12,color:muted,marginBottom:7}}>{item.name}</div><div style={{fontSize:21,color:item.color,fontWeight:600}}>{item.value}</div>
      </div>)}
    </div>
    <div style={{position:"absolute",left:48,right:48,bottom:47,height:1,backgroundColor:"#D5DDCE"}}/>
    <div style={{position:"absolute",left:48,bottom:22,fontSize:11,letterSpacing:2,color:muted}}>本地记录 / 自选 AI / 开源可查</div>
    <div style={{position:"absolute",right:48,bottom:22,fontSize:11,color:muted}}>功能动效示意 · 营养数值为演示数据</div>
  </AbsoluteFill>;
};

export const MyComposition = () => <Composition id="ShiJiIntro" component={ShiJiIntro} durationInFrames={120} fps={20} width={960} height={460}/>;
