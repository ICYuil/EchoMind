<template>
  <div class="row gap12 center" style="min-width:0;">
    <!--
      AvatarChip：可复用的小头像组件
      - avatarStyle：决定头像背景（渐变 or 上传图片）
      - cosmetics：来自“打卡奖励”的外观（头像框/勋章）
        这样你可以学到：父组件传 props -> 子组件根据 props 渲染 UI
    -->
    <div class="avatar-wrap" :class="cosmetics?.frame ? 'has-frame' : ''">
      <!-- 头像框（frame）：用一个“底层的装饰层”实现，不影响头像本身 -->
      <div v-if="cosmetics?.frame" class="frame" :class="cosmetics.frame.id" aria-hidden="true"></div>

      <!-- 头像本体 -->
      <div class="avatar" :style="avatarStyle"></div>

      <!-- 头像挂件（示例）：星环挂件（frame_orbit） -->
      <div v-if="cosmetics?.frame?.id==='frame_orbit'" class="ornament" :title="cosmetics.frame.name">
        {{ cosmetics.frame.icon }}
      </div>

      <!-- 勋章（badge）：右下角小徽章 -->
      <div v-if="cosmetics?.badge" class="avatar-badge" :title="cosmetics.badge.name">
        {{ cosmetics.badge.icon }}
      </div>
    </div>
    <!-- compact=true 时只显示头像（用于折叠侧边栏等场景） -->
    <div v-if="!compact" style="min-width:0;">
      <div class="name">{{ label || 'User' }}</div>
      <div class="sub">{{ sub || '' }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: String,
  sub: String,
  avatar: Object,
  // cosmetics 由 checkin 模块提供：{ frame: rewardObj|null, badge: rewardObj|null }
  cosmetics: Object,
  compact: { type: Boolean, default: false },
})

const avatarStyle = computed(() => {
  const a = props.avatar || { kind:'gradient', seed: props.sub || props.label || 'user' }
  if (a.kind === 'image' && a.src) return { backgroundImage: `url(${a.src})`, backgroundSize:'cover', backgroundPosition:'center' }
  const seed = (a.seed || 'user')
  const h = [...seed].reduce((s,c)=>s+c.charCodeAt(0),0) % 360
  return { background: `radial-gradient(24px 24px at 30% 30%, rgba(255,255,255,.85), rgba(255,255,255,.12)), linear-gradient(135deg, hsla(${h},85%,62%,.75), hsla(${(h+60)%360},85%,62%,.40))` }
})
</script>

<style scoped>
.avatar-wrap{
  position: relative;
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
}

.avatar{
  position: relative;
  z-index: 2;
  width: 44px;
  height: 44px;
  border-radius: 16px;
  border: 1px solid var(--stroke2);
  box-shadow: 0 14px 40px rgba(0,0,0,0.35);
}

/* frame：头像框的“底层装饰”，靠 CSS 实现不同风格 */
.frame{
  position: absolute;
  inset: -2px;
  border-radius: 18px;
  z-index: 1;
  opacity: 0.95;
}

/* 不同 frame 的样式（你可以继续扩展更多皮肤） */
.frame.frame_bronze{
  background: linear-gradient(135deg, rgba(215,136,70,0.55), rgba(255,218,168,0.22));
  box-shadow: 0 12px 30px rgba(0,0,0,0.35);
}

.frame.frame_orbit{
  background: linear-gradient(135deg, rgba(100,108,255,0.48), rgba(54,211,153,0.18));
  box-shadow: 0 12px 30px rgba(0,0,0,0.35);
}

/* 星环挂件：右上角的“装饰点” */
.ornament{
  position: absolute;
  z-index: 3;
  right: -6px;
  top: -8px;
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  border: 1px solid var(--stroke2);
  background: rgba(0,0,0,0.28);
  backdrop-filter: blur(10px);
  font-size: 14px;
}

/* 勋章：右下角的小徽章 */
.avatar-badge{
  position: absolute;
  z-index: 3;
  right: -6px;
  bottom: -8px;
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  border: 1px solid var(--stroke2);
  background: rgba(0,0,0,0.28);
  backdrop-filter: blur(10px);
  font-size: 14px;
}

.name{ font-weight: 950; line-height: 1.1; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.sub{ font-size: 12px; color: var(--muted); margin-top: 4px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
</style>
