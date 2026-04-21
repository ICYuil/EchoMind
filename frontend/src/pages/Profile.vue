<template>
  <div class="page-with-bg">
    <Topbar
      title="个人中心"
      subtitle="修改用户名 / 密码 / 头像；退出登录；注销账号（UI-only，本地存储）。"
    />

    <div class="container page-content">
      <div class="grid2">
        <div class="card" style="overflow:hidden;">
          <div style="padding:12px 14px; border-bottom:1px solid var(--stroke);">
            <div style="font-weight:950;">基本信息</div>
            <div class="muted2" style="font-size:12px; margin-top:4px;">这些信息仅保存在浏览器 localStorage</div>
          </div>

          <div style="padding:12px 14px 14px;">
            <div class="row gap12" style="align-items:flex-start;">
              <!-- cosmetics：把你在“打卡中心”装备的头像框/勋章展示出来 -->
              <AvatarChip
                :label="user?.username"
                :sub="user?.email"
                :avatar="user?.avatar"
                :cosmetics="cosmetics"
              />
              <div style="flex:1;">
                <div class="muted2" style="font-size:12px;">头像</div>
                <div class="row gap10 wrap" style="margin-top:8px;">
                  <button class="btn btn-glow" @click="setGradientAvatar">使用渐变头像</button>
                  <label class="btn ghost btn-glow">
                    上传图片
                    <input type="file" accept="image/*" style="display:none" @change="onPickFile" />
                  </label>
                </div>
                <div class="muted2" style="margin-top:10px; font-size:12px;">建议：上传方形图片效果更好</div>
              </div>
            </div>

            <div style="margin-top:14px;">
              <div class="muted2" style="font-size:12px; margin-bottom:6px;">用户名</div>
              <input class="input" v-model.trim="username" placeholder="输入新的用户名" />
              <div class="row gap10" style="margin-top:10px;">
                <button class="btn primary btn-glow" @click="saveProfile">保存资料</button>
              </div>
              <div v-if="okMsg" style="color:var(--ok); font-size:12px; margin-top:10px;">{{ okMsg }}</div>
              <div v-if="errMsg" style="color:var(--bad); font-size:12px; margin-top:10px;">{{ errMsg }}</div>
            </div>
          </div>
        </div>

        <!-- 新增：打卡奖励展示（不改变原有功能，只是多一个信息区块） -->
        <div class="card" style="overflow:hidden;">
          <div style="padding:12px 14px; border-bottom:1px solid var(--stroke);">
            <div style="font-weight:950;">成长奖励</div>
            <div class="muted2" style="font-size:12px; margin-top:4px;">来自“打卡中心”的解锁/装备</div>
          </div>
          <div style="padding:12px 14px 14px;">
            <div class="row gap14 wrap">
              <div class="kv" style="min-width:220px; flex:1;">
                <div>
                  <div class="k">连续打卡</div>
                  <div class="v">{{ streak }} 天</div>
                </div>
                <div class="muted2" style="font-size:12px;">streak</div>
              </div>
              <div class="kv" style="min-width:220px; flex:1;">
                <div>
                  <div class="k">累计打卡</div>
                  <div class="v">{{ total }} 天</div>
                </div>
                <div class="muted2" style="font-size:12px;">total</div>
              </div>
            </div>

            <div class="card soft" style="padding:12px 14px; margin-top:14px;">
              <div style="font-weight:950;">已装备</div>
              <div class="muted" style="font-size:12px; margin-top:8px; line-height:1.5;">
                头像框：{{ cosmetics?.frame?.name || '未装备' }}；勋章：{{ cosmetics?.badge?.name || '未装备' }}。
              </div>
              <div class="row gap10 wrap" style="margin-top:10px;">
                <RouterLink class="btn primary btn-glow" to="/app/checkin">去打卡中心管理</RouterLink>
              </div>
            </div>
          </div>
        </div>

        <div class="card" style="overflow:hidden;">
          <div style="padding:12px 14px; border-bottom:1px solid var(--stroke);">
            <div style="font-weight:950;">安全设置</div>
            <div class="muted2" style="font-size:12px; margin-top:4px;">修改密码 / 退出登录 / 注销账号</div>
          </div>

          <div style="padding:12px 14px 14px;">
            <div class="muted2" style="font-size:12px; margin-bottom:6px;">旧密码</div>
            <input class="input" type="password" v-model="oldPwd" />

            <div class="muted2" style="font-size:12px; margin:10px 0 6px;">新密码</div>
            <input class="input" type="password" v-model="newPwd" />

            <div class="row gap10 wrap" style="margin-top:10px;">
              <button class="btn btn-glow" @click="onChangePassword">修改密码</button>
              <button class="btn danger btn-glow" @click="onLogout">退出登录</button>
            </div>

            <div class="hr" style="margin:14px 0;"></div>

            <div class="card soft" style="padding:12px 14px;">
              <div style="font-weight:950; font-size:14px;">注销账号</div>
              <div class="muted" style="font-size:12px; margin-top:8px;">
                注销会清空当前账号登录状态（记录仍会保留在本地，你也可以扩展为同时清理记录）。
              </div>
              <div class="muted2" style="font-size:12px; margin:10px 0 6px;">输入密码确认</div>
              <input class="input" type="password" v-model="delPwd" />
              <div class="row gap10" style="margin-top:10px;">
                <button class="btn danger btn-glow" @click="onDelete">确认注销</button>
              </div>

              <div v-if="okMsg2" style="color:var(--ok); font-size:12px; margin-top:10px;">{{ okMsg2 }}</div>
              <div v-if="errMsg2" style="color:var(--bad); font-size:12px; margin-top:10px;">{{ errMsg2 }}</div>
            </div>

          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watchEffect } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import Topbar from '../components/ui/Topbar.vue'
import AvatarChip from '../components/ui/AvatarChip.vue'
import { currentUser, updateProfile, changePassword, logout, deleteAccount } from '../lib/auth'
import { getCheckinState, getEquippedCosmetics } from '../lib/checkin'

const router = useRouter()
const user = computed(() => currentUser())

// 打卡相关状态：用于展示连续天数 & 装备外观
const streak = ref(0)
const total = ref(0)
const cosmetics = ref({ frame: null, badge: null })

function refreshCheckin() {
  const email = user.value?.email
  if (!email) return
  const s = getCheckinState(email)
  streak.value = Number(s?.streak || 0)
  total.value = Number(s?.total || 0)
  cosmetics.value = getEquippedCosmetics(email)
}

const username = ref('')
const okMsg = ref('')
const errMsg = ref('')

const oldPwd = ref('')
const newPwd = ref('')
const delPwd = ref('')

const okMsg2 = ref('')
const errMsg2 = ref('')

watchEffect(() => {
  username.value = user.value?.username || ''
})

function onCheckinUpdated(ev){
  const email = ev?.detail?.email
  if (email && email === user.value?.email) refreshCheckin()
}

onMounted(() => {
  refreshCheckin()
  window.addEventListener('ai-checkin-updated', onCheckinUpdated)
  
  document.querySelectorAll('.btn-glow').forEach(btn => {
    btn.addEventListener('mousemove', (e) => {
      const rect = btn.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top
      btn.style.setProperty('--mouse-x', `${x}px`)
      btn.style.setProperty('--mouse-y', `${y}px`)
    })
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('ai-checkin-updated', onCheckinUpdated)
})

function flash(okRef, errRef, ok, err){
  okRef.value = ok || ''
  errRef.value = err || ''
  setTimeout(()=>{ okRef.value=''; errRef.value='' }, 2200)
}

async function saveProfile(){
  try{
    await updateProfile({ username: username.value })
    flash(okMsg, errMsg, '已保存', '')
  }catch(e){
    flash(okMsg, errMsg, '', e?.message || '保存失败')
  }
}

async function setGradientAvatar(){
  try{
    await updateProfile({ avatar: { kind:'gradient', seed: user.value?.email || 'user' } })
    flash(okMsg, errMsg, '已切换头像', '')
  }catch(e){
    flash(okMsg, errMsg, '', e?.message || '失败')
  }
}

async function onPickFile(ev){
  const f = ev.target.files?.[0]
  if (!f) return
  const reader = new FileReader()
  reader.onload = async () => {
    try{
      await updateProfile({ avatar: { kind:'image', src: String(reader.result) } })
      flash(okMsg, errMsg, '头像已更新', '')
    }catch(e){
      flash(okMsg, errMsg, '', e?.message || '失败')
    }
  }
  reader.readAsDataURL(f)
}

async function onChangePassword(){
  try{
    await changePassword({ oldPassword: oldPwd.value, newPassword: newPwd.value })
    oldPwd.value=''
    newPwd.value=''
    flash(okMsg2, errMsg2, '密码已修改', '')
  }catch(e){
    flash(okMsg2, errMsg2, '', e?.message || '修改失败')
  }
}

function onLogout(){
  logout()
  router.replace('/login')
}

async function onDelete(){
  try{
    await deleteAccount({ password: delPwd.value })
    router.replace('/register')
  }catch(e){
    flash(okMsg2, errMsg2, '', e?.message || '注销失败')
  }
}
</script>
