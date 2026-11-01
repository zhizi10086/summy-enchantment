# Third-Party Build Dependencies

本仓库在 `fabric/libs/` 和 `forge/libs/` 中保存少量未修改的第三方 JAR，仅用于离线编译、Mixin 目标校验和开发期兼容测试。它们不会被打入 Summy Enchantment 的发布 JAR，也不属于本仓库 MIT 许可范围。

| 文件 | 版本 / 平台 | 上游许可证 | SHA-256 |
| --- | --- | --- | --- |
| `kubejs-fabric-2001.6.5-build.16.jar` | KubeJS Fabric | GNU LGPLv3 | `D62FA3F080BF8B8B1037DD76DB4B17D12A9ECE5551B2B4CBCAAF059F81805D15` |
| `kubejs-forge-2001.6.5-build.16.jar` | KubeJS Forge | GNU LGPLv3 | `3DE6B7267D3AAB981848ED54D3AFE7EDF20532FA4060631FD6FFFCD99AC5F3D5` |
| `rhino-fabric-2001.2.3-build.10.jar` | Rhino Fabric | MPL-2.0 | `E205FAC40F7F27ABFA284B00F62D9C1CBD6DA494A64CB984EBADD9D76FF5F3EE` |
| `rhino-forge-2001.2.3-build.10.jar` | Rhino Forge | MPL-2.0 | `FED2211429301BF043864183CAB9AB8E92D4CC4DBB9E488CE6C75217C54584A6` |
| `spear-backport-fabric-1.4.7-1.20.1.jar` | Backported Spears Fabric | CC0-1.0（JAR 元数据） | `0BB965B95C59DB76EB4CD079FBD014ECDA103E3DF64ADD88FB38FC0491D802AD` |
| `spear-backport-forge-1.4.7-1.20.1.jar` | Backported Spears Forge | CC0-1.0（上游声明；JAR 元数据为占位文本） | `54849B707AC5ECB121C57A22F3DE43CF790C60B4F36D5763D1A5CBDB4DE2AE9D` |
| `summy-reliquary-1.8.5-forge.jar` | Summy Reliquary | Custom non-commercial | `64DFB15E6AC9CB675000465F7CF4AFB95178D27424EEBFDCA10FCDC2BF09993D` |

## Upstream

- KubeJS: <https://github.com/KubeJS-Mods/KubeJS>
- Rhino: <https://github.com/KubeJS-Mods/Rhino>
- Backported Spears: <https://www.mcmod.cn/class/23213.html>
- Summy Reliquary: <https://github.com/zhizi10086/summy-reliquary>

## License Texts

- GNU LGPLv3: <https://www.gnu.org/licenses/lgpl-3.0.txt>
- MPL-2.0: <https://www.mozilla.org/MPL/2.0/>
- CC0-1.0: <https://creativecommons.org/publicdomain/zero/1.0/>
- Summy Reliquary license: 以对应上游仓库中的 `LICENSE` 为准

第三方 JAR 保持原样，不修改、不重新打包。若升级依赖版本，应同时更新版本、来源、许可证和 SHA-256。
