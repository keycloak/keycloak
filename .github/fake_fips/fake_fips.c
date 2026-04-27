/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * https://github.com/torvalds/linux/blob/master/crypto/fips.c
 * https://pointer-overloading.blogspot.com/2013/09/linux-creating-entry-in-proc-file.html
 */

#include <linux/module.h>
#include <linux/sysctl.h>
#include <linux/version.h>

int fips_enabled = 1;

static struct ctl_table crypto_sysctl_table[] = {
	{
		.procname	= "fips_enabled",
		.data		= &fips_enabled,
		.maxlen		= sizeof(int),
		.mode		= 0444,
		.proc_handler	= proc_dointvec
	},
#if (LINUX_VERSION_CODE < KERNEL_VERSION(6, 11, 0))
	{}
#endif
};
static struct ctl_table crypto_dir_table[] = {
	{
		.procname       = "crypto",
		.mode           = 0555,
#if (LINUX_VERSION_CODE < KERNEL_VERSION(6, 4, 0))
		.child          = crypto_sysctl_table
#endif
	},
#if (LINUX_VERSION_CODE < KERNEL_VERSION(6, 11, 0))
	{}
#endif
};

static struct ctl_table_header *crypto_sysctls;

static void crypto_proc_fips_init(void)
{
#if (LINUX_VERSION_CODE < KERNEL_VERSION(6, 4, 0))
	crypto_sysctls = register_sysctl_table(crypto_dir_table);
#else
	crypto_sysctls = register_sysctl(crypto_dir_table->procname, crypto_sysctl_table);
#endif
}

static void crypto_proc_fips_exit(void)
{
	unregister_sysctl_table(crypto_sysctls);
}

static int __init fips_init(void)
{
	crypto_proc_fips_init();
	return 0;
}

static void __exit fips_exit(void)
{
	crypto_proc_fips_exit();
}

MODULE_LICENSE("GPL");
subsys_initcall(fips_init);
module_exit(fips_exit);
