import * as Yup from 'yup'
import {
  OS_LINUX,
  OS_WINDOWS,
  OS_MAC,
  DB_TYPE_WEB,
  DB_TYPE_OLTP,
  DB_TYPE_DW,
  DB_TYPE_DESKTOP,
  DB_TYPE_MIXED,
  SIZE_UNIT_MB,
  SIZE_UNIT_GB,
  HARD_DRIVE_HDD,
  HARD_DRIVE_SSD,
  HARD_DRIVE_SAN
} from 'reducers/configuration/constants'

const MAX_INTEGER = 9999
const MIN_MB_MEMORY = 512

const DB_TYPES = [
  DB_TYPE_WEB,
  DB_TYPE_OLTP,
  DB_TYPE_DW,
  DB_TYPE_DESKTOP,
  DB_TYPE_MIXED
]

const HARD_DRIVE_TYPES = [
  HARD_DRIVE_HDD,
  HARD_DRIVE_SSD,
  HARD_DRIVE_SAN
]

export const validationSchema = Yup.object().shape({
  osType: Yup.string()
    .required('必填项')
    .oneOf([OS_LINUX, OS_WINDOWS, OS_MAC], '不支持的操作系统类型'),
  dbType: Yup.string()
    .required('必填项')
    .oneOf(DB_TYPES, '不支持的数据库类型'),
  totalMemoryUnit: Yup.string()
    .required('必填项')
    .oneOf([SIZE_UNIT_MB, SIZE_UNIT_GB], '不支持的内存单位'),
  totalMemory: Yup.number()
    .required('必填项')
    .integer('必须为整型')
    .when('totalMemoryUnit', (totalMemoryUnit, schema) => {
      if (totalMemoryUnit === SIZE_UNIT_MB) {
        return schema.min(MIN_MB_MEMORY, `必须大于或等于 ${MIN_MB_MEMORY} MB`)
      }
      return schema.min(1, '必须大于 0')
    })
    .max(MAX_INTEGER, `必须小于或等于 ${MAX_INTEGER}`),
  hdType: Yup.string()
    .required('必填项')
    .oneOf(HARD_DRIVE_TYPES, '不支持的存储类型'),
  cpuNum: Yup.number()
    .notRequired()
    .integer('必须为整型')
    .min(1, '必须大于 0')
    .max(MAX_INTEGER, `必须小于或等于 ${MAX_INTEGER}`),
  connectionNum: Yup.number()
    .notRequired()
    .integer('必须为整型')
    .min(20, '必须大于或等于 20')
    .max(MAX_INTEGER, `必须大于或等于 ${MAX_INTEGER}`)
})
