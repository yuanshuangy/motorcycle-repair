<template>
  <div>
    <div class="page-header"><h2>我的评价</h2></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="shopName" label="维修店" width="140" />
        <el-table-column label="技师" width="100">
          <template #default="{row}">
            <span v-if="row.technicianName">{{ row.technicianName }}</span>
            <span v-else style="color:#999">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="180">
          <template #default="{row}">
            <el-rate v-model="row.rating" disabled />
            <el-tag v-if="row.rating>=4" type="success" size="small" style="margin-left:4px">👍好评</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="评价时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { reviewAPI } from '../../api'
import { useUserStore } from '../../store/user'
const userStore = useUserStore()
const list = ref([])
onMounted(async () => {
  try { const r=await reviewAPI.getPage({pageNum:1,pageSize:50,userId:userStore.userId}); if(r.code===200) list.value=r.data.records } catch(e){}
})
</script>
