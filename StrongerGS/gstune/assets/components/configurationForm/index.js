import React from 'react'
import {useDispatch} from 'react-redux'
import classnames from 'classnames'
import {Formik, Field, Form} from 'formik'
import FormField from 'components/form/field'
import FormDropdown from 'components/form/dropdown'
import TotalMemoryInput from './totalMemoryInput'
import {submitConfiguration} from 'reducers/configuration'
import {validationSchema} from './validation'
import {
  DEFAULT_DB_VERSION,
  DB_VERSIONS,
  OS_LINUX,
  OS_MAC,
  OS_WINDOWS,
  DB_TYPE_WEB,
  DB_TYPE_OLTP,
  DB_TYPE_DW,
  DB_TYPE_DESKTOP,
  DB_TYPE_MIXED,
  HARD_DRIVE_SSD,
  HARD_DRIVE_SAN,
  HARD_DRIVE_HDD,
  SIZE_UNIT_GB
} from 'reducers/configuration/constants'

import './configuration-form.css'

const dbVersionOptions = () => (
  DB_VERSIONS.map((version) => ({
    label: String(version),
    value: version
  }))
)

const osTypeOptions = () => (
  [
    {
      label: 'Linux',
      value: OS_LINUX
    },
    {
      label: 'OS X',
      value: OS_MAC
    },
    {
      label: 'Windows',
      value: OS_WINDOWS
    }
  ]
)

const dbTypeOptions = () => (
  [
    {
      label: 'Web应用',
      value: DB_TYPE_WEB
    },
    {
      label: '联机事务处理',
      value: DB_TYPE_OLTP
    },
    {
      label: '数据仓库',
      value: DB_TYPE_DW
    },
    {
      label: '桌面应用',
      value: DB_TYPE_DESKTOP
    },
    {
      label: '混合应用',
      value: DB_TYPE_MIXED
    }
  ]
)

const hdTypeOptions = () => (
  [
    {
      label: '固态硬盘存储',
      value: HARD_DRIVE_SSD
    },
    {
      label: '存储区域网络（SAN）',
      value: HARD_DRIVE_SAN
    },
    {
      label: '机械硬盘存储',
      value: HARD_DRIVE_HDD
    }
  ]
)

const ConfigurationForm = () => {
  const dispatch = useDispatch()

  const handleGenerateConfig = (values, {setSubmitting}) => {
    dispatch(submitConfiguration(values))
    setSubmitting(false)
  }

  return (
    <Formik
      onSubmit={handleGenerateConfig}
      initialValues={{
        dbVersion: DEFAULT_DB_VERSION,
        osType: OS_LINUX,
        dbType: DB_TYPE_WEB,
        cpuNum: '',
        totalMemory: '',
        totalMemoryUnit: SIZE_UNIT_GB,
        connectionNum: '',
        hdType: HARD_DRIVE_SSD
      }}
      validationSchema={validationSchema}
    >{({isSubmitting}) => (
        <Form>
{/*          <Field
            name="dbVersion"
            component={FormDropdown}
            label="数据库版本"
            options={dbVersionOptions()}
            tooltip="openGauss版本号"
          />*/}
          <Field
            name="osType"
            component={FormDropdown}
            label="操作系统类型"
            options={osTypeOptions()}
            tooltip="openGauss服务器主机的操作系统类型"
          />
          <Field
            name="dbType"
            component={FormDropdown}
            label="数据库类型"
            options={dbTypeOptions()}
            tooltip="openGauss数据库用于何种应用"
          />
          <TotalMemoryInput
            tooltip="openGauss可以使用多少内存"
          />
          <Field
            name="cpuNum"
            type="number"
            component={FormField}
            autoComplete="off"
            autoCorrect="off"
            autoCapitalize="none"
            min={1}
            max={9999}
            step={1}
            pattern="[0-9]{1,4}"
            placeholder="CPUs(CPU逻辑核数，可选)"
            label="CPU逻辑核数（CPUs）"
            tooltip={<span>openGauss能使用的CPU逻辑核数<br />CPUs = threads per core(单核线程数) * cores per socket(单CPU核数) * sockets(物理CPU个数)<br />Linux系统可以使用lscpu | grep -E '^Thread|^Core|^Socket|^CPU\('进行查询</span>}
          />
          <Field
            name="connectionNum"
            type="number"
            component={FormField}
            autoComplete="off"
            autoCorrect="off"
            autoCapitalize="none"
            min={20}
            max={9999}
            step={1}
            pattern="[0-9]{1,4}"
            placeholder="数据库连接数(可选)"
            label="数据库连接数"
            tooltip="openGauss客户端连接的最大数量"
          />
          <Field
            name="hdType"
            component={FormDropdown}
            label="数据存储"
            options={hdTypeOptions()}
            tooltip="数据存储设备的类型"
          />
          <div className="configuration-form-btn-wrapper">
            <button className={classnames('configuration-form-btn', {
              'configuration-form-btn--disabled': isSubmitting
            })} type="submit" disabled={isSubmitting}>生成openGauss数据库配置文件</button>
          </div>
        </Form>
      )}
    </Formik>
  )
}

export default ConfigurationForm
