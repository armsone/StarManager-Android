#!/bin/sh
# iOS 저장소의 브랜드 이미지·앱 아이콘 원본을 Android 리소스로 복사한다.
# (에이전트 환경에서 바이너리 복사가 불가능해 스크립트로 제공)
#
# 사용법:  sh scripts/copy-ios-assets.sh [iOS 저장소 경로]
set -eu

IOS_ROOT="${1:-../StarManager}"
ASSETS="$IOS_ROOT/StarManager/Assets.xcassets"
DEST="app/src/main/res/drawable-nodpi"

mkdir -p "$DEST"

cp "$ASSETS/ChatGPTBrand.imageset/chatgpt.jpg" "$DEST/brand_chatgpt.jpg"
cp "$ASSETS/GeminiBrand.imageset/gemini.jpg"   "$DEST/brand_gemini.jpg"
cp "$ASSETS/GrokBrand.imageset/grok.jpg"       "$DEST/brand_grok.jpg"

# 앱 아이콘 원본(1024px). mipmap PNG로 쓰려면 별도 리사이즈가 필요하다.
mkdir -p "app/src/main/res/drawable-nodpi"
cp "$ASSETS/AppIcon.appiconset/starmanager-app-icon.png" "$DEST/starmanager_app_icon.png"

echo "복사 완료: $DEST"
echo "브랜드 아이콘은 앱이 런타임에 자동으로 감지해 사용합니다."
