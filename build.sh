#!/usr/bin/env bash
# =============================================================================
# Widget Factory 构建脚本
# 用法:
#   ./build.sh              # 构建 debug APK
#   ./build.sh release      # 构建 release APK（未签名，需自行签名）
#   ./build.sh clean        # 清理构建产物
#   ./build.sh clean build  # 清理后构建 debug
# =============================================================================
set -euo pipefail

# ---- 颜色输出 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# ---- 定位工程根目录（脚本所在目录） ----
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"
info "工程目录: $PROJECT_DIR"

# ---- Termux 环境检测 ----
# Termux 特征：$PREFIX 指向 /data/data/com.termux/files/usr，且有 $TERMUX_VERSION
IS_TERMUX=0
if [ -n "${TERMUX_VERSION:-}" ] || [ "${PREFIX:-}" = "/data/data/com.termux/files/usr" ]; then
    IS_TERMUX=1
    info "检测到 Termux 环境 (Android)"
fi

# 跨平台解析真实路径（Termux toybox 的 readlink 不支持 -f）
real_path() {
    if command -v realpath >/dev/null 2>&1; then
        realpath "$1" 2>/dev/null || echo "$1"
    elif readlink -f "$1" >/dev/null 2>&1; then
        readlink -f "$1"
    else
        # 最简降级：直接返回原路径
        echo "$1"
    fi
}

# 确保某命令存在；Termux 下尝试 pkg install
require_tool() {
    local tool="$1" pkg="${2:-$1}"
    if command -v "$tool" >/dev/null 2>&1; then
        return 0
    fi
    if [ "$IS_TERMUX" -eq 1 ]; then
        warn "Termux 缺少 $tool，尝试 pkg install $pkg ..."
        pkg install -y "$pkg" 2>/dev/null || {
            error "安装 $pkg 失败，请手动执行: pkg install $pkg"
            exit 1
        }
    else
        error "缺少 $tool，请先安装后重试（或设置相应环境变量）"
        exit 1
    fi
}

# ---- 1. 检查 Gradle Wrapper ----
if [ ! -f "gradlew" ]; then
    error "未找到 gradlew，请确认工程根目录下存在 gradlew / gradle/wrapper/gradle-wrapper.jar"
    exit 1
fi
chmod +x gradlew

# ---- 2. 探测 / 自动下载 JDK 17 ----
# 本地缓存目录：JDK 下载到工程内 .tools/jdk-17，避免污染系统
JDK_CACHE_DIR="$PROJECT_DIR/.tools/jdk-17"
REQUIRED_JAVA_MAJOR=17

# 工具：提取 java 主版本号（兼容 1.8.0 / 11.0.x / 17.0.x / 21 等格式）
java_major_version() {
    local ver
    ver="$("$1/bin/java" -version 2>&1 | awk -F'"' '/version/ {print $2}')"
    # 1.8.0 -> 8; 11.0.x -> 11; 17.0.x -> 17
    case "$ver" in
        1.*) echo "$ver" | cut -d. -f2 ;;
        *)   echo "$ver" | cut -d. -f1 ;;
    esac
}

# 尝试从已有 JAVA_HOME 或 PATH 中找 JDK 17+
find_existing_jdk17() {
    local candidate=""
    # 1) JAVA_HOME
    if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
        candidate="$JAVA_HOME"
    # 2) PATH 中的 java
    elif command -v java >/dev/null 2>&1; then
        local java_bin
        java_bin="$(command -v java)"
        [ -L "$java_bin" ] && java_bin="$(real_path "$java_bin")"
        candidate="$(dirname "$(dirname "$java_bin")")"
    fi

    # 3) Termux: openjdk-17 安装在 $PREFIX/lib/jvm/java-17-openjdk
    if [ "$IS_TERMUX" -eq 1 ] && [ -z "$candidate" ]; then
        for jdk in "$PREFIX/lib/jvm/java-17-openjdk" "$PREFIX/lib/jvm/java-21-openjdk"; do
            if [ -x "${jdk}/bin/java" ]; then
                candidate="$jdk"
                break
            fi
        done
    fi

    if [ -n "$candidate" ] && [ -x "${candidate}/bin/java" ]; then
        local major
        major="$(java_major_version "$candidate")"
        if [ "$major" -ge "$REQUIRED_JAVA_MAJOR" ] 2>/dev/null; then
            echo "$candidate"
            return 0
        fi
    fi
    return 1
}

# 下载并安装 JDK 17
# - Termux：用 pkg install openjdk-17（bionic libc，Temurin glibc 版跑不起来）
# - 其他：下载 Eclipse Temurin JDK 17 到本地缓存
download_jdk17() {
    # ---- Termux 分支 ----
    if [ "$IS_TERMUX" -eq 1 ]; then
        info "Termux 环境：通过 pkg 安装 openjdk-17（Termux 为 bionic libc，glibc 版 JDK 无法运行）"
        if ! command -v pkg >/dev/null 2>&1; then
            error "Termux 下未找到 pkg 包管理器，无法自动安装 JDK"
            exit 1
        fi
        pkg update -y 2>/dev/null || true
        if ! pkg install -y openjdk-17 2>&1; then
            error "openjdk-17 安装失败。请手动执行: pkg install openjdk-17"
            exit 1
        fi
        # openjdk-17 安装后 java 在 $PREFIX/bin，JAVA_HOME 为 $PREFIX/lib/jvm/java-17-openjdk
        local termux_jdk="$PREFIX/lib/jvm/java-17-openjdk"
        if [ ! -x "${termux_jdk}/bin/java" ]; then
            # 兜底：从 java 命令反推
            if command -v java >/dev/null 2>&1; then
                local jb; jb="$(command -v java)"
                [ -L "$jb" ] && jb="$(real_path "$jb")"
                termux_jdk="$(dirname "$(dirname "$jb")")"
            fi
        fi
        if [ -x "${termux_jdk}/bin/java" ]; then
            info "Termux JDK 17 安装完成: $termux_jdk"
            echo "$termux_jdk"
            return 0
        else
            error "JDK 安装后仍未找到 java，请检查 pkg install openjdk-17 是否成功"
            exit 1
        fi
    fi

    # ---- 非 Termux：下载 Temurin ----
    info "未找到 JDK 17+，将自动下载 Eclipse Temurin JDK 17 到 $JDK_CACHE_DIR"

    # 确保 curl 可用（非 Termux 下若缺失则报错）
    require_tool curl curl

    # 探测 OS 与架构
    local os arch
    case "$(uname -s)" in
        Linux*)  os="linux" ;;
        Darwin*) os="mac" ;;
        MINGW*|MSYS*|CYGWIN*) os="windows" ;;
        *) error "不支持的操作系统: $(uname -s)"; exit 1 ;;
    esac
    case "$(uname -m)" in
        x86_64|amd64) arch="x64" ;;
        aarch64|arm64) arch="aarch64" ;;
        *) error "不支持的架构: $(uname -m)"; exit 1 ;;
    esac

    # 若缓存已存在且可用，直接复用
    if [ -x "${JDK_CACHE_DIR}/bin/java" ]; then
        local major
        major="$(java_major_version "$JDK_CACHE_DIR")"
        if [ "$major" -ge "$REQUIRED_JAVA_MAJOR" ] 2>/dev/null; then
            info "复用已缓存的 JDK 17: $JDK_CACHE_DIR"
            echo "$JDK_CACHE_DIR"
            return 0
        fi
    fi

    mkdir -p "$(dirname "$JDK_CACHE_DIR")"
    local archive="$JDK_CACHE_DIR.tmp.tar"
    [ "$os" = "windows" ] && archive="$JDK_CACHE_DIR.tmp.zip"

    # Adoptium API：获取 17 LTS 最新 GA 版二进制（302 重定向到 GitHub 资产）
    local url="https://api.adoptium.net/v3/binary/latest/${REQUIRED_JAVA_MAJOR}/ga/${os}/${arch}/jdk/hotspot/normal/eclipse"
    info "下载 JDK 17 (${os}/${arch}) ..."

    # 支持代理（继承 HTTP_PROXY/HTTPS_PROXY）
    if ! curl -fL --connect-timeout 30 --retry 3 --retry-delay 5 -o "$archive" "$url"; then
        rm -f "$archive"
        error "JDK 下载失败。请检查网络/代理，或手动安装 JDK 17 并设置 JAVA_HOME。"
        exit 1
    fi

    info "解压 JDK ..."
    rm -rf "$JDK_CACHE_DIR"
    mkdir -p "$JDK_CACHE_DIR"
    if [ "$os" = "windows" ]; then
        require_tool unzip unzip
        unzip -q "$archive" -d "$JDK_CACHE_DIR"
    else
        require_tool tar tar
        tar -xzf "$archive" -C "$JDK_CACHE_DIR" --strip-components=1
    fi
    rm -f "$archive"

    if [ ! -x "${JDK_CACHE_DIR}/bin/java" ]; then
        error "JDK 解压异常，未找到 bin/java"
        exit 1
    fi

    info "JDK 17 下载完成: $JDK_CACHE_DIR"
    echo "$JDK_CACHE_DIR"
}

# 优先使用已有 JDK 17+，否则自动下载
if FOUND_JDK="$(find_existing_jdk17)"; then
    JAVA_HOME="$FOUND_JDK"
    info "使用已有 JDK: $JAVA_HOME"
else
    warn "未检测到 JDK 17+（AGP 8.5 要求）。"
    JAVA_HOME="$(download_jdk17)"
fi
export JAVA_HOME
info "JDK 版本: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"

# ---- 3. 配置 Android SDK（local.properties） ----
# 优先级: 环境变量 ANDROID_HOME / ANDROID_SDK_ROOT > 已有 local.properties > 自动探测
SDK_DIR=""
if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME" ]; then
    SDK_DIR="$ANDROID_HOME"
elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
    SDK_DIR="$ANDROID_SDK_ROOT"
elif [ -f "local.properties" ]; then
    SDK_DIR="$(grep -E '^sdk.dir=' local.properties | head -1 | cut -d= -f2-)"
fi

# 常见路径自动探测
if [ -z "$SDK_DIR" ] || [ ! -d "$SDK_DIR" ]; then
    # Termux 常见 SDK 路径 + 通用路径
    for candidate in \
        "$HOME/android-sdk" \
        "$PREFIX/share/android-sdk" \
        "$HOME/Android/Sdk" \
        "$HOME/Library/Android/sdk" \
        "/opt/android-sdk" \
        "/usr/lib/android-sdk" \
        "/Android/Sdk"; do
        if [ -n "$candidate" ] && [ -d "$candidate" ]; then
            SDK_DIR="$candidate"
            info "自动探测 Android SDK: $SDK_DIR"
            break
        fi
    done
fi

if [ -z "$SDK_DIR" ] || [ ! -d "$SDK_DIR" ]; then
    if [ "$IS_TERMUX" -eq 1 ]; then
        error "未找到 Android SDK。Termux 下请先安装: pkg install android-sdk  或手动下载 SDK 后设置 ANDROID_HOME"
    else
        error "未找到 Android SDK。请设置 ANDROID_HOME 环境变量，或在工程下创建 local.properties 写入: sdk.dir=/path/to/android-sdk"
    fi
    exit 1
fi

# 写入/更新 local.properties（避免编码问题，用正斜杠）
echo "sdk.dir=${SDK_DIR}" > local.properties
info "已写入 local.properties: sdk.dir=$SDK_DIR"

# 检查必要的 SDK 组件
if [ ! -d "$SDK_DIR/platforms" ]; then
    warn "SDK 下未找到 platforms 目录，可能需要通过 SDK Manager 安装 compileSdk=34 平台"
fi
if [ ! -d "$SDK_DIR/build-tools" ]; then
    warn "SDK 下未找到 build-tools 目录，可能需要安装 build-tools"
fi

# ---- 4. 代理支持（可选） ----
# 若环境存在 HTTP_PROXY/HTTPS_PROXY，自动传给 Gradle JVM
GRADLE_OPTS_EXTRA=""
if [ -n "${HTTPS_PROXY:-}" ] || [ -n "${https_proxy:-}" ]; then
    PROXY_URL="${HTTPS_PROXY:-${https_proxy:-}}"
    # 解析 host:port（支持 http://host:port 形式）
    PROXY_HOST="$(echo "$PROXY_URL" | sed -E 's#^[a-z]+://##; s#/.*$##; s#:[0-9]+$##')"
    PROXY_PORT="$(echo "$PROXY_URL" | sed -E 's#^[a-z]+://##; s#/.*$##' | grep -oE ':[0-9]+$' | tr -d ':')"
    if [ -n "$PROXY_HOST" ] && [ -n "$PROXY_PORT" ]; then
        GRADLE_OPTS_EXTRA="-Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT"
        info "检测到代理 $PROXY_HOST:$PROXY_PORT，已注入 Gradle"
    fi
fi
export GRADLE_OPTS="${GRADLE_OPTS:-} $GRADLE_OPTS_EXTRA"

# ---- 5. 解析构建参数 ----
TASK="assembleDebug"
VARIANT="debug"
DO_CLEAN=0

for arg in "$@"; do
    case "$arg" in
        clean)
            DO_CLEAN=1
            ;;
        release)
            TASK="assembleRelease"
            VARIANT="release"
            ;;
        debug)
            TASK="assembleDebug"
            VARIANT="debug"
            ;;
        *)
            warn "未知参数: $arg（忽略）"
            ;;
    esac
done

# ---- 6. 执行构建 ----
if [ "$DO_CLEAN" -eq 1 ]; then
    info "清理构建产物..."
    ./gradlew clean
fi

info "开始构建: $TASK"
set +e
./gradlew "$TASK" --stacktrace
BUILD_RC=$?
set -e

# ---- 7. 收集产物 ----
APK_DIR="app/build/outputs/apk/$VARIANT"
if [ "$BUILD_RC" -eq 0 ]; then
    info "构建成功 ✓"
    if [ -d "$APK_DIR" ]; then
        APK_FILE="$(find "$APK_DIR" -name "*.apk" -type f | head -1)"
        if [ -n "$APK_FILE" ]; then
            # 兼容 Termux toybox du（不保证支持 -h），用 wc -c 取字节数
            APK_BYTES="$(wc -c < "$APK_FILE" 2>/dev/null || echo 0)"
            APK_SIZE="$((APK_BYTES / 1024)) KB"
            echo ""
            echo -e "${GREEN}========================================${NC}"
            echo -e "${GREEN} APK 产物:${NC} $APK_FILE"
            echo -e "${GREEN} 大小:${NC}     $APK_SIZE"
            echo -e "${GREEN}========================================${NC}"
        else
            warn "构建成功但未在 $APK_DIR 找到 apk 文件"
        fi
    else
        warn "构建成功但未找到输出目录 $APK_DIR"
    fi
else
    error "构建失败 (exit=$BUILD_RC)，请查看上方日志"
    exit "$BUILD_RC"
fi
