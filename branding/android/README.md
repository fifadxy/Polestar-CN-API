# PsAssistant Android icon

The black front-facing vehicle supplied by the project owner is the shared Android app identity, starting with v0.6.1-beta1. Keep using this icon for subsequent Android phone and watch variants. Do not substitute the previous star/vector icon or a generic vehicle. This rule changes the app identity, not vehicle model diagrams or the required monochrome system notification status glyph.

- `psassistant-icon-master.png`: canonical 1254 × 1254 edited artwork, based on the supplied icon. The car was made slightly smaller with image editing; retain this master rather than regenerating its vehicle in future builds.
- `../../android-app/app/src/main/res/drawable-nodpi/psassistant_icon_art.webp`: 512 × 512 lossless WebP runtime copy. Resample with equal horizontal and vertical scale. Do not stretch, crop, or independently change the car's proportions.
- `ps_icon_foreground.xml`: applies equal 12.962963% insets to the square runtime art, equivalent to 80 × 80 dp centered in the standard 108 × 108 dp adaptive foreground. This padding is proportional, so it is retained at every launcher icon size.
- `mipmap-anydpi-v26/ic_launcher.xml`: black background plus the common foreground. Both `android:icon` and `android:roundIcon` reference it. There is no unrelated star monochrome layer.
- `drawable/ic_launcher.xml`: fallback drawable reuses the same artwork. The app header loads the actual application icon rather than maintaining a separate app mark.

The outer decorative frame is background artwork and may be masked by a launcher. Keep the vehicle, mirrors, headlights and wheels inside the central 66 dp safe circle of the 108 dp foreground. Review representative circle, rounded-square and squircle masks before changing the inset or master. OEM motion/theming effects still require device checks.

Runtime asset conversion (ImageMagick):

```sh
magick branding/android/psassistant-icon-master.png -resize 512x512 -define webp:lossless=true android-app/app/src/main/res/drawable-nodpi/psassistant_icon_art.webp
```

The high-resolution master belongs in this branding directory and is not packaged in the APK. APK resources use only the smaller runtime copy.


按钮公用资源及后续迭代遵循 [Psa 按钮图标规范 v1](../../docs/Psa_Button_Icon_Design_Specification_v1.md)，图册见 icons/Psa_Control_Icon_Catalog_v1.svg。该规范以用户新增参考为依据；启动器黑武士图标保持原约定。
