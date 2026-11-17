# 最终采用的模型与素材

2026-11-18 定稿。本清单只列当前采用的两款模型，历史三层调料架、小木桌、独立岛台不属于交接素材。下载地址、字节数和 SHA-256 见 [model-assets.json](model-assets.json)。

## 1. 冰箱

[Oven and fridge doors 来源页](https://1designtool.com/3d/models/kitchen-oven-and-fridge-doors) · [下载 GLB](https://assets.stoicsoft.com/1designtool/3d/models/kitchen-oven-and-fridge-doors/077490a3a8/model.glb)

- 下载后命名为 `kitchen-oven-and-fridge-doors.glb`，原文件 132,008 字节。
- 保留 `fridge_body`、`fridge_door`、`freezer_door`；隐藏 `housing`、`oven` 及其子网格。
- 文件有 8 秒 `scene-motion` 展示动画，初始主门已开，不能直接循环作为点击开关。定稿渲染清除源动画，主门从 0 度到约 −100 度（Blender Z 轴）旋转；下门关闭。
- 上下门作为同一个冰箱入口。源模型预置食物是装饰，与真实库存不一一映射。

## 2. 厨房操作台

[Counter and cabinet modules 来源页](https://1designtool.com/3d/models/kitchen-counter-and-cabinet-modules) · [下载 GLB](https://assets.stoicsoft.com/1designtool/3d/models/kitchen-counter-and-cabinet-modules/b3618a921a/model.glb)

- 下载后命名为 `kitchen-counter-and-cabinet-modules.glb`，原文件 115,584 字节，无动画。
- 保留 `module_1`、`module_2`、`sink`，以及 `props_tile_white_light` 中的三个瓶身、`props_steel` 中的瓶盖。
- 保留 `carcass`、`worktop` 的原模型 x≤0 部分，删除右侧两组柜体及锅、砧板。封闭切边，补同材质右端板；保持原比例，成品约宽 1.23 m、深 0.624 m、台面高 0.90 m。
- 保留完整水槽与龙头。三个瓶子共用调料入口，不需要拆成三个独立对象，也不固定绑定三种调料。
- 完整截取操作与位置以 [render_scene.py](render_scene.py) 为准；不要误用历史“保留右侧两组柜体”的版本。

## 许可与交付方式

两款均来自 1DesignTool Kitchen 系列。[当前库许可](https://1designtool.com/3d/license)于 2026-11-18 复核：可免费用于个人及商业项目、修改、项目渲染及随 App 交付，无强制署名要求；不允许把原始或修改后的模型文件单独再分发，或作为模型包／素材库发布。因此原 GLB 保留在本机研究目录，仓库不复制原 GLB 或裁剪后的 GLB。本交接包含项目设计渲染、来源、校验值及复现脚本，后续开发从上述来源自行获取同版本模型。

许可不是 CC0。不要把禁止源文件单独分发误解为禁止使用渲染图或随产品交付。正式获取素材时仍需查看来源许可是否变化。

## 已交付渲染

- `frames/00.png`—`frames/19.png`：480 × 640 RGBA，20 帧，含冰箱、裁剪后水槽台和地面阴影；0 为关门，19 为开门。反向可预览关门。
- `pantry-fridge-design.svg` 和 `.svg.png`：可编辑三状态对照图与预览。
- `screen-closed.png`、`screen-open.png`、`screen-spices.png`、`screen-item.png`、`screen-empty.png`：主要界面截图。
- [design-manifest.json](design-manifest.json)：当前定稿与帧校验值；[scene-layout.json](scene-layout.json)：模型投影范围；真实点击热区见主交接文档。

## 如何重新渲染

使用 Blender 5.2.3 LTS。将上面两款 GLB 分别下载并按指定名称放入同一目录；`MODELS_DIR` 与 `OUTPUT_DIR` 指向自己的素材目录和新输出目录，然后执行：

```sh
blender --background --threads 4 --python render_scene.py -- \
  --models-dir "$MODELS_DIR" --output-dir "$OUTPUT_DIR"
```

只检查首尾帧时追加 `--frames 0 19`。输出目录会生成 `frames` 和 `scene-layout.json`；不会下载文件、写入 App 或自动覆盖设计截图与清单。默认 20 帧渲染；源文件原点、节点命名及变换依赖指定版本，先核对 SHA-256。

脚本已从制作本稿的脚本整理为参数化文件；本轮用独立输出目录验证首尾帧。重新渲染可能因 Blender／渲染环境出现像素差异，几何、镜头和门的角度以脚本为准，现有 PNG 校验值用于校验交接文件完整性。
