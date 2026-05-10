<template>
  <div class="avatar-uploader">
    <div class="avatar-uploader__preview" @click="triggerUpload">
      <el-avatar
        :size="previewSize"
        :src="currentAvatar"
        class="avatar-uploader__image"
      >
        <span v-if="!currentAvatar" class="avatar-uploader__placeholder">
          <el-icon><Plus /></el-icon>
        </span>
      </el-avatar>
      <div class="avatar-uploader__overlay">
        <el-icon><Camera /></el-icon>
        <span>更换头像</span>
      </div>
    </div>

    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      class="avatar-uploader__input"
      @change="handleFileChange"
    />

    <el-dialog
      v-model="dialogVisible"
      title="裁剪头像"
      width="500px"
      :close-on-click-modal="false"
      class="avatar-uploader__dialog"
    >
      <div class="cropper-container">
        <img
          ref="cropperRef"
          :src="previewUrl"
          class="cropper-container__image"
        />
      </div>
      <div class="cropper-container__tips">
        <el-icon><InfoFilled /></el-icon>
        <span>拖拽调整裁剪区域，支持滚轮缩放</span>
      </div>
      <template #footer>
        <div class="cropper-container__actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="uploading" @click="handleUpload">
            确认上传
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="loadingVisible"
      title="上传中"
      width="300px"
      :show-close="false"
      class="avatar-uploader__loading"
    >
      <div class="upload-loading">
        <el-progress
          type="circle"
          :percentage="uploadProgress"
          :stroke-width="10"
        />
        <p class="upload-loading__text">正在上传头像...</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Plus, Camera, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import Cropper from 'cropperjs'
import { uploadImage } from '@/api/upload'

const props = defineProps<{
  modelValue?: string
  size?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const fileInput = ref<HTMLInputElement>()
const cropperRef = ref<HTMLImageElement>()
const dialogVisible = ref(false)
const loadingVisible = ref(false)
const previewUrl = ref('')
const uploading = ref(false)
const uploadProgress = ref(0)
let cropper: Cropper | null = null

const previewSize = computed(() => props.size || 120)
const currentAvatar = computed(() => props.modelValue || '')

function triggerUpload() {
  fileInput.value?.click()
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过5MB')
    return
  }

  previewUrl.value = URL.createObjectURL(file)
  dialogVisible.value = true

  target.value = ''
}

watch(dialogVisible, (visible) => {
  if (visible) {
    setTimeout(() => {
      initCropper()
    }, 100)
  } else {
    if (cropper) {
      cropper.destroy()
      cropper = null
    }
  }
})

function initCropper() {
  const image = cropperRef.value
  if (!image) return

  if (cropper) {
    cropper.destroy()
  }

  cropper = new Cropper(image, {
    aspectRatio: 1,
    viewMode: 1,
    dragMode: 'move',
    autoCropArea: 0.8,
    restore: false,
    guides: true,
    center: true,
    highlight: false,
    cropBoxMovable: true,
    cropBoxResizable: true,
    toggleDragModeOnDblclick: false,
    ready() {
      const containerData = cropper!.getContainerData()
      const cropBoxData = cropper!.getCropBoxData()
      const minSize = Math.min(containerData.width, containerData.height) * 0.3
      if (cropBoxData.width < minSize || cropBoxData.height < minSize) {
        cropper!.setCropBoxData({
          width: minSize,
          height: minSize
        })
      }
    }
  })
}

function buildCenterSquareCanvas(image: HTMLImageElement): HTMLCanvasElement {
  const sourceWidth = image.naturalWidth || image.width
  const sourceHeight = image.naturalHeight || image.height
  const squareSize = Math.min(sourceWidth, sourceHeight)
  const offsetX = Math.floor((sourceWidth - squareSize) / 2)
  const offsetY = Math.floor((sourceHeight - squareSize) / 2)

  const canvas = document.createElement('canvas')
  canvas.width = 512
  canvas.height = 512

  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('浏览器不支持 Canvas')
  }

  ctx.imageSmoothingEnabled = true
  ctx.imageSmoothingQuality = 'high'
  ctx.drawImage(image, offsetX, offsetY, squareSize, squareSize, 0, 0, 512, 512)
  return canvas
}

async function handleUpload() {
  if (!cropper) return

  uploading.value = true
  loadingVisible.value = true

  try {
    let canvas: HTMLCanvasElement
    const cropperInstance = cropper as any
    if (typeof cropperInstance.getCroppedCanvas === 'function') {
      canvas = cropperInstance.getCroppedCanvas({
        maxWidth: 512,
        maxHeight: 512,
        imageSmoothingEnabled: true,
        imageSmoothingQuality: 'high'
      })
    } else {
      if (!cropperRef.value) {
        throw new Error('裁剪区域未初始化')
      }
      // 兼容 cropperjs 新版本：若 API 变化，退化为中心正方形裁剪，避免上传失败
      canvas = buildCenterSquareCanvas(cropperRef.value)
    }

    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (b) => {
          if (b) resolve(b)
          else reject(new Error('Failed to create blob'))
        },
        'image/jpeg',
        0.9
      )
    })

    const file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })

    uploadProgress.value = 30

    const res: any = await uploadImage(file)

    uploadProgress.value = 100

    emit('update:modelValue', res.data)
    ElMessage.success('头像上传成功')

    dialogVisible.value = false
    loadingVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败，请重试')
  } finally {
    uploading.value = false
    loadingVisible.value = false
    uploadProgress.value = 0
  }
}
</script>

<style scoped lang="scss">
// Cropper.js 核心样式
.cropper-container {
  position: relative;
  width: 100%;
  font-size: 0;
  line-height: 0;
  touch-action: none;
  user-select: none;

  img {
    display: block;
    min-width: 0 !important;
    max-width: none !important;
    max-height: none !important;
  }
}

.cropper-wraper,
.cropper-canvas {
  position: absolute;
  overflow: hidden;
}

.cropper-wraper {
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.cropper-canvas {
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.cropper-modal {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: hsla(0, 0%, 0%, 0.5);
}

.cropper-crop-box {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.cropper-view-box {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  outline: 2px solid hsl(225, 60%, 50%);
  outline-color: hsla(225, 60%, 50%, 0.75);
}

.cropper-dashed {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: 0 dashed hsl(0, 0%, 100%);
  opacity: 0.5;
}

.cropper-dashed-h {
  border-top-width: 1px;
  border-bottom-width: 1px;
}

.cropper-dashed-v {
  border-left-width: 1px;
  border-right-width: 1px;
}

.cropper-center {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;

  &::before,
  &::after {
    content: '';
    position: absolute;
    background-color: hsla(0, 0%, 100%, 0.75);
  }

  &::before {
    top: 0;
    left: 50%;
    width: 1px;
    height: 100%;
    transform: translateX(-50%);
  }

  &::after {
    top: 50%;
    left: 0;
    width: 100%;
    height: 1px;
    transform: translateY(-50%);
  }
}

.cropper-face,
.cropper-line,
.cropper-point {
  position: absolute;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
}

.cropper-face {
  top: 0;
  left: 0;
  background-color: hsla(0, 0%, 100%, 0.1);
}

.cropper-line,
.cropper-point {
  opacity: 1;
}

.cropper-line {
  background-color: hsla(0, 0%, 100%, 0.5);

  &-e {
    top: 0;
    right: -3px;
    width: 5px;
    cursor: ew-resize;
  }

  &-n {
    top: -3px;
    left: 0;
    height: 5px;
    cursor: ns-resize;
  }

  &-w {
    top: 0;
    left: -3px;
    width: 5px;
    cursor: ew-resize;
  }

  &-s {
    bottom: -3px;
    left: 0;
    height: 5px;
    cursor: ns-resize;
  }
}

.cropper-point {
  width: 10px;
  height: 10px;
  background-color: hsla(0, 0%, 100%, 0.75);
  opacity: 1;
  border-radius: 50%;

  &::before {
    content: '';
    position: absolute;
    width: 100%;
    height: 100%;
    background-color: hsl(225, 60%, 50%);
    opacity: 0.75;
    border-radius: 50%;
  }

  &-nw { top: -5px; left: -5px; cursor: nwse-resize; }
  &-nw::before { top: 50%; left: 50%; }
  &-ne { top: -5px; right: -5px; cursor: nesw-resize; }
  &-ne::before { top: 50%; right: 50%; transform: translate(50%, -50%); }
  &-sw { bottom: -5px; left: -5px; cursor: nesw-resize; }
  &-sw::before { bottom: 50%; left: 50%; transform: translate(-50%, 50%); }
  &-se { bottom: -5px; right: -5px; cursor: nwse-resize; }
  &-se::before { bottom: 50%; right: 50%; transform: translate(50%, 50%); }
}

.cropper-hide {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.cropper-hidden {
  display: none !important;
}

.cropper-move {
  cursor: move;
}

.cropper-crop {
  cursor: crosshair;
}

.cropper-disabled .cropper-face,
.cropper-disabled .cropper-line,
.cropper-disabled .cropper-point {
  cursor: not-allowed;
}
.avatar-uploader {
  &__preview {
    position: relative;
    display: inline-block;
    cursor: pointer;
    border-radius: 50%;
    overflow: hidden;

    &:hover {
      .avatar-uploader__overlay {
        opacity: 1;
      }
    }
  }

  &__image {
    display: block;
    border: 3px solid hsl(220, 10%, 92%);
    transition: border-color 0.2s ease;
  }

  &__overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    background: hsla(220, 10%, 20%, 0.6);
    color: #fff;
    font-size: 12px;
    opacity: 0;
    transition: opacity 0.2s ease;

    .el-icon {
      font-size: 20px;
    }
  }

  &__placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background: hsl(220, 10%, 95%);
    color: hsl(220, 10%, 55%);
    font-size: 24px;
  }

  &__input {
    display: none;
  }
}

.cropper-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;

  &__image {
    max-width: 100%;
    max-height: 400px;
    display: block;
  }

  &__tips {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    background: hsl(220, 10%, 96%);
    border-radius: 8px;
    font-size: 13px;
    color: hsl(220, 10%, 55%);

    .el-icon {
      color: hsl(220, 60%, 50%);
    }
  }

  &__actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

.upload-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px 0;

  &__text {
    margin: 0;
    font-size: 14px;
    color: hsl(220, 10%, 45%);
  }
}

:deep(.el-dialog__header) {
  padding: 16px 20px;
  border-bottom: 1px solid hsl(220, 10%, 92%);
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-dialog__footer) {
  padding: 16px 20px;
  border-top: 1px solid hsl(220, 10%, 92%);
}
</style>
