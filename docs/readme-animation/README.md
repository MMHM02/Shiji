# 食记 README 动画

使用 Remotion 4.0.534 制作的 6 秒循环品牌动效，960 × 460，20 fps。由餐盘转为营养圆环，末尾回到起始状态。画面属于功能示意，营养数字为演示数据，不是应用截图。

## 预览与导出

在此目录运行：

```sh
npm ci
npm run dev -- --no-open
npm run lint
npm run render
```

输出文件：`../images/shiji-intro.gif`，由仓库根目录的 README 引用。

- `src/Composition.tsx`：文字、图形、时间与配色。
- 时长和画布参数在同文件的 `Composition` 注册处。
- 所有动画由帧驱动，不依赖 CSS animation 或外部图片服务。
- 字体使用系统 Microsoft YaHei；在没有该字体的系统上渲染时，请配置已安装的中文字体以避免缺字。
- README 提供静态封面链接，方便不希望观看循环动画的读者。

项目自有动画源码遵循仓库 Apache-2.0 许可；Remotion 及其他依赖遵循各自许可。
