## makefile automates the build and deployment for lein projects

# location of the http://github.com/plandes/clj-zenbuild cloned directory
ZBHOME?=	../clj-zenbuild

# clean the generated app assembly file
ADD_CLEAN+=	$(ASBIN_DIR)

all:		info

include $(ZBHOME)/src/mk/compile.mk
include $(ZBHOME)/src/mk/dist.mk
include $(ZBHOME)/src/mk/release.mk
