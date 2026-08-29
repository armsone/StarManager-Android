#!/bin/sh
# iOS 저장소의 브랜드 이미지·앱 아이콘 원본을 Android 리소스로 복사한다.
# (에이전트 환경에서 바이너리 복사가 불가능해 스크립트로 제공)
#
# 사용법:  sh scripts/copy-ios-assets.sh [iOS 저장소 경로]
set -eu

if [ -n "${1:-}" ]; then
  IOS_ROOT="$1"
elif [ -d "../iManagerAI" ]; then
  IOS_ROOT="../iManagerAI"
elif [ -d "../iManager" ]; then
  IOS_ROOT="../iManager"
else
  IOS_ROOT="../iManagerAI"
fi

if [ -d "$IOS_ROOT/iManagerAI/Assets.xcassets" ]; then
  ASSETS="$IOS_ROOT/iManagerAI/Assets.xcassets"
elif [ -d "$IOS_ROOT/iManager/Assets.xcassets" ]; then
  ASSETS="$IOS_ROOT/iManager/Assets.xcassets"
else
  ASSETS="$IOS_ROOT/iManagerAI/Assets.xcassets"
fi

DEST="app/src/main/res/drawable-nodpi"

mkdir -p "$DEST"

if [ -f "$ASSETS/ChatGPTBrand.imageset/chatgpt.jpg" ]; then cp "$ASSETS/ChatGPTBrand.imageset/chatgpt.jpg" "$DEST/brand_chatgpt.jpg"; fi
if [ -f "$ASSETS/GeminiBrand.imageset/gemini.jpg" ]; then cp "$ASSETS/GeminiBrand.imageset/gemini.jpg"   "$DEST/brand_gemini.jpg"; fi
if [ -f "$ASSETS/GrokBrand.imageset/grok.jpg" ]; then cp "$ASSETS/GrokBrand.imageset/grok.jpg"       "$DEST/brand_grok.jpg"; fi

# 앱 아이콘 원본(1024px). mipmap PNG로 쓰려면 별도 리사이즈가 필요하다.
if [ -f "$ASSETS/AppIcon.appiconset/imanagerai-app-icon.png" ]; then
  cp "$ASSETS/AppIcon.appiconset/imanagerai-app-icon.png" "$DEST/imanagerai_app_icon.png"
elif [ -f "$ASSETS/AppIcon.appiconset/imanager-app-icon.png" ]; then
  cp "$ASSETS/AppIcon.appiconset/imanager-app-icon.png" "$DEST/imanagerai_app_icon.png"
elif [ -f "$ASSETS/AppIcon.appiconset/starmanager-app-icon.png" ]; then
  cp "$ASSETS/AppIcon.appiconset/starmanager-app-icon.png" "$DEST/imanagerai_app_icon.png"
fi

echo "복사 완료: $DEST"
echo "브랜드 아이콘은 앱이 런타임에 자동으로 감지해 사용합니다."
